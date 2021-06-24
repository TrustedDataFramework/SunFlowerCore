package org.tdf.sunflower.net;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.util.FastByteComparisons;
import org.tdf.common.util.HashUtil;
import org.tdf.common.util.HexBytes;
import org.tdf.common.util.LogLock;
import org.tdf.sunflower.facade.ConsensusEngine;
import org.tdf.sunflower.proto.Code;
import org.tdf.sunflower.proto.Message;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * message filter
 */
@Slf4j(topic = "net")
public class MessageFilter implements Plugin {
    private final ConsensusEngine consensusEngine;
    private final Cache<HexBytes, Boolean> cache;
    private final Map<HexBytes, Messages> multiPartCache = new HashMap<>();
    private final PeerServerConfig config;
    private final Lock multiPartCacheLock = new LogLock(new ReentrantLock(), "p2p-mp");

    MessageFilter(PeerServerConfig config, ConsensusEngine consensusEngine) {
        this.cache = CacheBuilder.newBuilder()
            .maximumSize(config.getMaxPeers() * 8).build();
        this.config = config;
        Executors.newSingleThreadScheduledExecutor()
            .scheduleWithFixedDelay(() -> {
                multiPartCacheLock.lock();
                long now = System.currentTimeMillis() / 1000;
                try {
                    multiPartCache.entrySet().removeIf(
                        entry -> now - entry.getValue().writeAt > config.getCacheExpiredAfter()
                    );
                } finally {
                    multiPartCacheLock.unlock();
                }
            }, config.getCacheExpiredAfter(), config.getCacheExpiredAfter(), TimeUnit.SECONDS);
        this.consensusEngine = consensusEngine;
    }

    @Override
    public void onMessage(ContextImpl context, PeerServerImpl server) {
        // cache multi part message
        if (!context.getRemote().getProtocol().equals(server.getSelf().getProtocol())) {
            log.error("protocol not match received = {}, while {} expected", context.getRemote().getProtocol(), server.getSelf().getProtocol());
            context.block();
            return;
        }
        if (context.message.getCode() == Code.MULTI_PART) {
            multiPartCacheLock.lock();
            long now = System.currentTimeMillis() / 1000;
            HexBytes key = HexBytes.fromBytes(context.message.getSignature().toByteArray());
            try {
                Messages messages =
                    multiPartCache.getOrDefault(
                        key,
                        new Messages(
                            new Message[(int) context.message.getTtl()],
                            (int) context.message.getTtl(),
                            now
                        )
                    );
                messages.multiParts[(int) context.message.getNonce()] = context.message;
                multiPartCache.put(key, messages);
                if (messages.size() == messages.total) {
                    multiPartCache.remove(key);
                    server.onMessage(messages.merge(), context.channel);
                }

            } finally {
                multiPartCacheLock.unlock();
            }
            return;
        }

        // filter invalid signatures

        // reject peer in black list and not in whitelist
        if (config.isBlocked(context.remote.getID())) {
            log.error("the peer " + context.remote + " has been blocked");
            context.disconnect();
            return;
        }

        // reject blocked peer
        if (server.getClient().peersCache.hasBlocked(context.remote)) {
            log.error("the peer " + context.remote + " has been blocked");
            context.disconnect();
            return;
        }
        // filter message from your self
        if (context.getRemote().equals(server.getSelf())) {
            log.error("message received from yourself");
            context.exit();
            return;
        }
        // filter message which ttl < 0
        if (context.message.getTtl() < 0) {
            log.error("receive message ttl less than 0");
            context.exit();
            return;
        }
        HexBytes hash = HexBytes.fromBytes(HashUtil.sha3(Util.getRawForSign(context.message)));

        // filter message had been received
        if (cache.asMap().containsKey(hash)) {
            context.exit();
        }
        log.debug("receive " + context.message.getCode()
            + " from " +
            context.remote.getHost() + ":" + context.remote.getPort()
        );
        cache.put(hash, true);
    }

    @Override
    public void onStart(PeerServerImpl server) {

    }

    @Override
    public void onNewPeer(PeerImpl peer, PeerServerImpl server) {

    }

    @Override
    public void onDisconnect(PeerImpl peer, PeerServerImpl server) {

    }

    @Override
    public void onStop(PeerServerImpl server) {

    }

    @AllArgsConstructor
    private static class Messages {
        private final Message[] multiParts;
        private final int total;
        private final long writeAt;

        public int size() {
            return (int) Arrays.stream(multiParts).filter(Objects::nonNull).count();
        }

        @SneakyThrows
        public Message merge() {
            int byteArraySize = Arrays.stream(multiParts).map(x -> x.getBody().size())
                .reduce(0, Integer::sum);

            byte[] total = new byte[byteArraySize];

            int current = 0;
            for (Message part : multiParts) {
                byte[] p = part.getBody().toByteArray();
                System.arraycopy(p, 0, total, current, p.length);
                current += p.length;
            }

            if (!FastByteComparisons.equal(
                HashUtil.sha3(total),
                multiParts[0].getSignature().toByteArray())
            ) {
                throw new RuntimeException("merge failed");
            }

            return Message.parseFrom(total);
        }
    }
}
