package org.tdf.sunflower.net;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.serialize.Codecs;
import org.tdf.common.store.MapStore;
import org.tdf.common.store.Store;
import org.tdf.common.store.StoreWrapper;
import org.tdf.common.util.HexBytes;
import org.tdf.crypto.CryptoContext;
import org.tdf.sunflower.Start;
import org.tdf.sunflower.exception.PeerServerInitException;
import org.tdf.sunflower.facade.PeerServerListener;
import org.tdf.sunflower.proto.Code;
import org.tdf.sunflower.proto.Message;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class PeerServerImpl implements ChannelListener, PeerServer {
    private PeerServerConfig config;
    private List<Plugin> plugins = new CopyOnWriteArrayList<>();
    private Client client;
    private PeerImpl self;
    private MessageBuilder builder;
    private NetLayer netLayer;

    // if non-database provided, use memory database
    Store<String, String> peerStore = new MapStore<>();

    public PeerServerImpl() {
    }

    // persistent storage to store peers
    public PeerServerImpl withStore(@NonNull Store<byte[], byte[]> persistentStore) {
        this.peerStore = new StoreWrapper<>(persistentStore,
                Codecs.STRING,
                Codecs.STRING);
        return this;
    }

    @Override
    public Peer getSelf() {
        return self;
    }

    @Override
    public void dial(Peer peer, byte[] message) {
        client.dial(peer, builder.buildAnother(message, peer.getID().getBytes()));
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
    public void addListeners(PeerServerListener... peerServerListeners) {
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
        log.info("your p2p private key is " + HexBytes.encode(self.getPrivateKey()));
        if (config.getBootstraps() != null) {
            client.bootstrap(config.getBootstraps());
        }
        if (config.getTrusted() != null) {
            client.trust(config.getTrusted());
        }
        // connect to stored peers when server restarts
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
    public void init(Properties properties) throws PeerServerInitException {
        JavaPropsMapper mapper = new JavaPropsMapper();
        try {
            config = mapper.readPropertiesAs(properties, PeerServerConfig.class);
            if (config.getMaxTTL() <= 0) config.setMaxTTL(PeerServerConfig.DEFAULT_MAX_TTL);
            if (config.getMaxPeers() <= 0) config.setMaxPeers(PeerServerConfig.DEFAULT_MAX_PEERS);
        } catch (Exception e) {
            String schema = "";
            try {
                // create a example properties for error log
                schema = mapper.writeValueAsProperties(
                        PeerServerConfig.builder()
                                .bootstraps(Collections.singletonList(new URI("node://localhost:9955")))
                                .build()
                ).toString();
            } catch (Exception ignored) {
            }
            throw new PeerServerInitException(
                    "load properties failed :" + properties.toString() + " expecting " + schema
            );
        }
        if (!config.isEnableDiscovery() &&
                Stream.of(config.getBootstraps(), config.getTrusted())
                        .filter(Objects::nonNull)
                        .map(List::size).reduce(0, Integer::sum) == 0
        ) {
            throw new PeerServerInitException("cannot connect to any peer fot the discovery " +
                    "is disabled and none bootstraps and trusted provided");
        }
        try {
            resolveSelf();
        } catch (Exception e) {
            e.printStackTrace();
            throw new PeerServerInitException("failed to load peer server invalid address " + config.getAddress());
        }
        if ("websocket".equals(config.getName().trim().toLowerCase())) {
            netLayer = new WebSocketNetLayer(self.getPort());
        } else {
            netLayer = new GRpcNetLayer(self.getPort());
        }
        netLayer.setHandler((c) -> c.addListeners(client, this));
        builder = new MessageBuilder(self);
        client = new Client(self, config, builder, netLayer).withListener(this);

        // loading plugins
        plugins.add(new MessageFilter(config));
        if (config.isEnableMessageLog()) {
            plugins.add(new MessageLogger());
        }
        plugins.add(new PeersManager(config));
    }

    private void resolveSelf() throws Exception {
        // find valid private key from properties
        byte[] sk = config.getPrivateKey() == null ? null : config.getPrivateKey().getBytes();
        if(sk == null || sk.length == 0){
            sk = peerStore.get("self").map(HexBytes::decode)
                    .orElse(null);
        }
        if(sk == null || sk.length == 0){
            sk = CryptoContext.generateKeyPair().getPrivateKey().getEncoded();
        }

        this.self = PeerImpl.createSelf(config.getAddress(), sk);

        // generate a new private key when not found
        peerStore.put("self", HexBytes.encode(sk));
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
            channel.close("failed to parse peer " + message.getRemotePeer());
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

    @Override
    public void stop() {
        plugins.forEach(x -> x.onStop(this));
        client.peersCache
                .getChannels()
                .forEach(x -> x.close("application will shutdown"));
        try {
            netLayer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("peer server closed");
    }
}
