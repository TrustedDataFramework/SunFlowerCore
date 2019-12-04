package org.tdf.sunflower.net;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.tdf.common.*;
import org.tdf.exception.PeerServerLoadException;
import org.tdf.serialize.Serializers;
import org.tdf.store.StoreWrapper;
import org.tdf.sunflower.Start;
import org.tdf.sunflower.proto.Code;
import org.tdf.sunflower.proto.Message;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class PeerServerImpl implements Channel.ChannelListener, PeerServer {
    public static None NONE = new None();

    private PeerServerConfig config;
    private List<Plugin> plugins = new ArrayList<>();
    private Client client;
    private PeerImpl self;
    private MessageBuilder builder;
    private NetLayer netLayer;
    Store<String, String> peerStore;

    public PeerServerImpl() {
    }

    public PeerServerImpl withStore(BatchAbleStore<byte[], byte[]> persistentStore) {
        this.peerStore = new StoreWrapper<>(persistentStore,
                Serializers.STRING,
                Serializers.STRING);
        return this;
    }

    @Override
    public Peer getSelf() {
        return self;
    }

    @Override
    public void dial(Peer peer, byte[] message) {
        client.dial(peer, builder.buildAnother(message));
    }

    @Override
    public void broadcast(byte[] message) {
        client.broadcast(
                builder.buildMessage(Code.ANOTHER, config.getMaxTTL(), message)
        );
    }

    @Override
    public List<Peer> getBootStraps() {
        return new ArrayList<>(client.peersCache.bootstraps.keySet());
    }

    @Override
    public List<Peer> getPeers() {
        return client.peersCache.getPeers().collect(Collectors.toList());
    }

    @Override
    public void use(PeerServerListener... peerServerListeners) {
        for (PeerServerListener listener : peerServerListeners) {
            plugins.add(new PluginWrapper(listener));
        }
    }


    @Override
    public void start() {
        plugins.forEach(l -> l.onStart(this));
        netLayer.start();
        resolveHost();
        log.info("peer server is listening on " +
                self.encodeURI());
        log.info("your p2p secret address is " +
                String.format("%s://%s@%s:%d",
                        self.getProtocol(),
                        new HexBytes(
                                self.getPrivateKey().getEncoded(),
                                self.getPrivateKey().generatePublicKey().getEncoded())
                        ,
                        self.getHost(),
                        self.getPort()));
        if (config.getBootstraps() != null) {
            client.bootstrap(config.getBootstraps());
        }
        if (config.getTrusted() != null) {
            client.trust(config.getTrusted());
        }
        Optional<String> o = Optional.ofNullable(peerStore).flatMap(x -> x.get("peers"));
        if (!o.isPresent()) return;
        String peers = o.get();
        try {
            URI[] uris = Start.MAPPER.readValue(peers, URI[].class);
            for (URI uri : uris) {
                client.dial(uri.getHost(), uri.getPort(), builder.buildPing());
            }
        } catch (JsonProcessingException ignored) {
        }
    }

    @Override
    public void load(Properties properties) throws PeerServerLoadException {
        JavaPropsMapper mapper = new JavaPropsMapper();
        try {
            config = mapper.readPropertiesAs(properties, PeerServerConfig.class);
            if (config.getMaxTTL() <= 0) config.setMaxTTL(PeerServerConfig.DEFAULT_MAX_TTL);
            if (config.getMaxPeers() <= 0) config.setMaxPeers(PeerServerConfig.DEFAULT_MAX_PEERS);
            if (config.getName() == null) config.setName(PeerServerConfig.DEFAULT_NAME);
        } catch (Exception e) {
            String schema = "";
            try {
                schema = mapper.writeValueAsProperties(
                        PeerServerConfig.builder()
                                .bootstraps(Collections.singletonList(new URI("node://localhost:9955")))
                                .build()
                ).toString();
            } catch (Exception ignored) {
            }
            throw new PeerServerLoadException(
                    "load properties failed :" + properties.toString() + " expecting " + schema
            );
        }
        if (!config.isEnableDiscovery() &&
                Stream.of(config.getBootstraps(), config.getTrusted())
                        .filter(Objects::nonNull)
                        .map(List::size).reduce(0, Integer::sum) == 0
        ) {
            throw new PeerServerLoadException("cannot connect to any peer fot the discovery " +
                    "is disabled and none bootstraps and trusted provided");
        }
        try {
            parseSelf();
        } catch (Exception e) {
            e.printStackTrace();
            throw new PeerServerLoadException("failed to load peer server invalid address " + config.getAddress());
        }
        if ("websocket".equals(config.getName().trim().toLowerCase())) {
            netLayer = new WebSocketNetLayer(self.getPort());
        } else {
            netLayer = new GRpcNetLayer(self.getPort());
        }
        netLayer.onChannelIncoming((c) -> c.addListener(client, this));
        builder = new MessageBuilder(self);
        client = new Client(self, config, builder, netLayer).withListener(this);

        // loading plugins
        plugins.add(new MessageFilter(config));
        if (config.isEnableMessageLog()) {
            plugins.add(new MessageLogger());
        }
        plugins.add(new PeersManager(config));
    }

    private void parseSelf() throws Exception {
        if (self == null && config.getAddress().getRawUserInfo() != null && !config.getAddress().getRawUserInfo().equals("")) {
            self = PeerImpl.create(config.getAddress(), new HexBytes(config.getAddress().getRawUserInfo()).getBytes());
        }
        Optional<String> selfPrivateKey = peerStore == null ? Optional.empty() : peerStore.get("self");
        if (self == null && selfPrivateKey.isPresent()) {
            self = PeerImpl.create(config.getAddress(), new HexBytes(selfPrivateKey.get()).getBytes());
        }
        if (self == null){
            self = PeerImpl.create(config.getAddress());
        }
        if(peerStore != null){
            peerStore.put("self",
                    Hex.encodeHexString(self.getPrivateKey().getEncoded())
                    + Hex.encodeHexString(self.getPrivateKey().generatePublicKey().getEncoded())
            );
        }
    }

    @Override
    public void onConnect(PeerImpl remote, Channel channel) {
        for (Plugin plugin : plugins) {
            plugin.onNewPeer(remote, this);
        }
    }

    @Override
    public void onMessage(Message message, Channel channel) {
        Optional<PeerImpl> peer = channel.getRemote();
        if (!peer.isPresent()) {
            channel.close();
            throw new RuntimeException("failed to parse peer");
        }
        ContextImpl context = ContextImpl.builder()
                .channel(channel)
                .client(client)
                .message(message)
                .builder(builder)
                .remote(peer.get()).build();

        for (Plugin plugin : plugins) {
            plugin.onMessage(context, this);
        }
    }

    @Override
    public void onError(Throwable throwable, Channel channel) {

    }

    @Override
    public void onClose(Channel channel) {
        if (channel.getRemote().isPresent()) {
            for (Plugin plugin : plugins) {
                plugin.onDisconnect(channel.getRemote().get(), this);
            }
        }
    }

    Client getClient() {
        return client;
    }

    private void resolveHost() {
        if (!self.getHost().equals("localhost") && !self.getHost().equals("127.0.0.1")) {
            return;
        }
        String externalIP = null;
        try {
            externalIP = Util.externalIp();
        } catch (Exception ignored) {
            log.error("cannot get external ip, fall back to bind ip");
        }
        if (externalIP != null && Util.ping(externalIP, self.getPort())) {
            log.info("ping " + externalIP + " success, set as your host");
            self.setHost(externalIP);
            return;
        }
        String bindIP = null;
        try {
            bindIP = Util.bindIp();
        } catch (Exception e) {
            log.error("get bind ip failed");
        }
        if (bindIP != null) {
            self.setHost(bindIP);
        }
    }
}
