package org.tdf.sunflower.net;

import com.google.common.base.Functions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.store.BatchStore;
import org.tdf.sunflower.ApplicationConstants;
import org.tdf.sunflower.proto.Disconnect;
import org.tdf.sunflower.proto.Peers;

import java.util.AbstractMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j(topic = "net")
// plugin for peers join/remove management
public class PeersManager implements Plugin {
    private final PeerServerConfig config;
    private final ConcurrentHashMap<PeerImpl, Boolean> pending = new ConcurrentHashMap<>();
    private PeerServerImpl server;
    private ScheduledExecutorService executorService;

    PeersManager(PeerServerConfig config) {
        this.config = config;
    }

    @Override
    @SneakyThrows
    public void onMessage(ContextImpl context, PeerServerImpl server) {
        Client client = server.getClient();
        PeersCache cache = client.peersCache;
        MessageBuilder builder = client.messageBuilder;
        context.keep();
        switch (context.message.getCode()) {
            case PING:
                context.channel.write(builder.buildPong());
                return;
            case LOOK_UP:
                context.channel.write(
                        builder.buildPeers(server.getPeers())
                );
                return;
            case PEERS:
                if (!config.isEnableDiscovery()) return;
                Peers.parseFrom(context.message.getBody()).getPeersList().stream()
                        .map(PeerImpl::parse)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .filter(x -> !cache.contains(x) && !x.equals(server.getSelf()) && x.getProtocol().equals(server.getSelf().getProtocol()))
                        .forEach(x -> pending.put(x, true));
                return;
            case DISCONNECT:
                String reason = Disconnect.parseFrom(context.message.getBody()).getReason();
                if (reason != null && !reason.isEmpty())
                    log.error("disconnect from peer " + context.getRemote() + " reason is " + reason);
                context.channel.close();
                return;
        }
    }

    @Override
    public void onStart(PeerServerImpl server) {
        this.server = server;
        Client client = server.getClient();
        PeersCache cache = client.peersCache;
        MessageBuilder builder = client.messageBuilder;

        // keep self alive
        int core = Runtime.getRuntime().availableProcessors();
        executorService = Executors.newScheduledThreadPool(
                core > 1 ? core / 2 : core,
                new ThreadFactoryBuilder().setNameFormat("peers-manger-thread-%d").build()
        );

        executorService.scheduleWithFixedDelay(() -> client.broadcast(
                builder.buildPing()
        ), 0, config.getDiscoverRate(), TimeUnit.SECONDS);

        executorService
                .scheduleWithFixedDelay(() -> {

                    ((BatchStore<String, String>) server.peerStore)
                            .putAll(
                                    client.peersCache.getPeers()
                                            .map(p -> new AbstractMap.SimpleEntry<>(p.getID().toHex(), p.encodeURI()))
                                            .collect(Collectors.toList())
                            );

                    lookup();
                    cache.half();
                    if (!config.isEnableDiscovery()) return;

                    pending.keySet()
                            .stream()
                            .filter(x -> !cache.contains(x))
                            .limit(config.getMaxPeers())
                            .forEach(
                                    p -> {
                                        log.info("try to connect to peer " + p);
                                        client.dial(p, builder.buildPing());
                                    }
                            );
                    pending.clear();
                }, 0, config.getDiscoverRate(), TimeUnit.SECONDS);
    }

    private void lookup() {
        Client client = server.getClient();
        PeersCache cache = client.peersCache;
        MessageBuilder builder = client.messageBuilder;

        if (!config.isEnableDiscovery()) {
            // keep channel to bootstraps and trusted alive
            Stream.of(cache.bootstraps.keySet().stream(), cache.trusted.keySet().stream())
                    .flatMap(Functions.identity())
                    .filter(x -> !cache.contains(x))
                    .forEach(x -> server.getClient().dial(x, builder.buildPing()));
            return;
        }
        // query for neighbours when neighbours is not empty
        if (cache.size() > 0) {
            client.broadcast(builder.buildLookup());
            cache.trusted.keySet().forEach(p -> client.dial(p, builder.buildPing()));
            return;
        }
        // query for peers from bootstraps and trusted when neighbours is empty
        Stream.of(cache.bootstraps, cache.trusted)
                .flatMap(x -> x.keySet().stream())
                .forEach(p -> client.dial(p, builder.buildLookup()));
    }

    @Override
    public void onNewPeer(PeerImpl peer, PeerServerImpl server) {

    }

    @Override
    public void onDisconnect(PeerImpl peer, PeerServerImpl server) {

    }

    @Override
    public void onStop(PeerServerImpl server) {
        executorService.shutdown();
        try {
            executorService.awaitTermination(ApplicationConstants.MAX_SHUTDOWN_WAITING, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
