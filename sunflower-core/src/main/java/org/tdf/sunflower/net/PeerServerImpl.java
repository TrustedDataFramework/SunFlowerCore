package org.tdf.sunflower.net;

import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.store.JsonStore;
import org.tdf.sunflower.facade.ConsensusEngine;
import org.tdf.sunflower.facade.PeerServerListener;
import org.tdf.sunflower.facade.SecretStore;
import org.tdf.sunflower.proto.Message;
import org.tdf.sunflower.types.CryptoContext;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j(topic = "net")
public class PeerServerImpl implements ChannelListener, PeerServer {
    // if non-database provided, use memory database
    final JsonStore peerStore;
    final ConsensusEngine consensusEngine;
    final SecretStore secretStore;
    private final List<Plugin> plugins = new CopyOnWriteArrayList<>();
    private PeerServerConfig config;
    private Client client;
    private PeerImpl self;
    private MessageBuilder builder;
    private NetLayer netLayer;

    public PeerServerImpl(JsonStore peerStore, ConsensusEngine consensusEngine, SecretStore secretStore) {
        this.peerStore = peerStore;
        this.consensusEngine = consensusEngine;
        this.secretStore = secretStore;
    }

    @Override
    public boolean isFull() {
        return client.peersCache.isFull();
    }

    @Override
    public Peer getSelf() {
        return self;
    }

    @Override
    public void dial(Peer peer, byte[] message) {
        builder.buildAnother(message, 1, peer)
                .forEach(m -> client.dial(peer, m));
    }

    @Override
    public void broadcast(byte[] message) {
        client.peersCache.getChannels()
                .filter(ch -> ch.getRemote().isPresent())
                .forEach(ch ->
                        builder.buildAnother(message, config.getMaxTTL(), ch.getRemote().get())
                                .forEach(ch::write)
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
        if (config.getBootstraps() != null) {
            client.bootstrap(config.getBootstraps());
        }
        if (config.getTrusted() != null) {
            client.trust(config.getTrusted());
        }
        // connect to stored peers when server restarts
        peerStore.forEach(k -> {
            if ("self".equals(k.getKey()))
                return;
            PeerImpl peer = PeerImpl.parse(k.getValue().asText()).get();
            client.dial(peer.getHost(), peer.getPort(), builder.buildPing());
        });
    }

    @Override
    public void init(Properties properties) {
        JavaPropsMapper mapper = new JavaPropsMapper();
        try {
            config = mapper.readPropertiesAs(properties, PeerServerConfig.class);
            if (config.getMaxTTL() <= 0) config.setMaxTTL(PeerServerConfig.DEFAULT_MAX_TTL);
            if (config.getMaxPeers() <= 0) config.setMaxPeers(PeerServerConfig.DEFAULT_MAX_PEERS);
            if (secretStore != SecretStore.NONE)
                config.setPrivateKey(secretStore.getPrivateKey());

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
            throw new RuntimeException(
                    "load properties failed :" + properties.toString() + " expecting " + schema
            );
        }


        if (!config.isEnableDiscovery() &&
                Stream.of(config.getBootstraps(), config.getTrusted())
                        .filter(Objects::nonNull)
                        .map(List::size).reduce(0, Integer::sum) == 0
        ) {
            log.warn("cannot connect to any peer for the discovery " +
                    "is disabled and none bootstraps and trusted provided");
        }
        try {
            resolveSelf();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("failed to load peer server invalid address " + config.getAddress());
        }
        builder = new MessageBuilder(self, config);
        if ("grpc".equals(config.getName().trim().toLowerCase())) {
            netLayer = new GRpcNetLayer(self.getPort(), builder);
        } else {
            netLayer = new WebSocketNetLayer(self.getPort(), builder);
        }
        netLayer.setHandler((c) -> c.addListeners(client, this));
        client = new Client(self, config, builder, netLayer).withListener(this);

        // loading plugins
        plugins.add(new MessageFilter(config, consensusEngine));
        plugins.add(new MessageLogger());
        plugins.add(new PeersManager(config));
    }

    private void resolveSelf() {
        // find valid private key from 1.properties 2.persist 3. generate
        byte[] sk = config.getPrivateKey() == null ? null : config.getPrivateKey().getBytes();
        if (sk == null || sk.length == 0) {
            sk = CryptoContext.generateSecretKey();
        }

        this.self = PeerImpl.createSelf(config.getAddress(), sk);
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
            if (context.exited)
                break;
            try {
                plugin.onMessage(context, this);
            } catch (Exception e) {
                e.printStackTrace();
            }
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
