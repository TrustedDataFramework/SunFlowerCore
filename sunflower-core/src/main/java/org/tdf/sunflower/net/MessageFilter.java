package org.tdf.sunflower.net;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.util.FastByteComparisons;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.account.Address;
import org.tdf.sunflower.crypto.CryptoContext;
import org.tdf.sunflower.facade.ConsensusEngine;
import org.tdf.sunflower.proto.Code;
import org.tdf.sunflower.proto.Message;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * message filter
 */
@Slf4j(topic = "net")
public class MessageFilter implements Plugin {
    private Cache<HexBytes, Boolean> cache;
    private Map<HexBytes, Messages> multiPartCache = new HashMap<>();
    private final ConsensusEngine consensusEngine;

    @AllArgsConstructor
    private static class Messages {
        private Message[] multiParts;
        private int total;

        public int size() {
            return (int) Arrays.stream(multiParts).filter(Objects::nonNull).count();
        }

        private long writeAt;

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
                    CryptoContext.digest(total),
                    multiParts[0].getSignature().toByteArray())
            ) {
                throw new RuntimeException("合并失败");
            }

            return Message.parseFrom(total);
        }
    }

    private Lock multiPartCacheLock = new ReentrantLock();


    MessageFilter(PeerServerConfig config, ConsensusEngine consensusEngine) {
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(config.getMaxPeers() * 8).build();
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
        if (!CryptoContext.verifySignature(
                context.getRemote().getID().getBytes(),
                Util.getRawForSign(context.message),
                context.message.getSignature().toByteArray()
        )) {
            log.error("invalid signature received from " + context.remote);
            context.exit();
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
        HexBytes k = HexBytes.fromBytes(context.message.getSignature().toByteArray());
        // filter message had been received
        if (cache.asMap().containsKey(k)) {
            context.exit();
        }
        log.debug("receive " + context.message.getCode()
                + " from " +
                context.remote.getHost() + ":" + context.remote.getPort()
        );
        cache.put(k, true);
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
}
