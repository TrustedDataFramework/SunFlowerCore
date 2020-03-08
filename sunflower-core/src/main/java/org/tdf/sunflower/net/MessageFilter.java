package org.tdf.sunflower.net;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.crypto.CryptoContext;

/**
 * message filter
 */
@Slf4j(topic = "net")
public class MessageFilter implements Plugin {
    private Cache<HexBytes, Boolean> cache;

    MessageFilter(PeerServerConfig config) {
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(config.getMaxPeers() * 8).build();
    }

    @Override
    public void onMessage(ContextImpl context, PeerServerImpl server) {
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
        if (server.getClient().peersCache.hasBlocked(context.remote)){
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
