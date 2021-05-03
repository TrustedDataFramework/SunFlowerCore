package org.tdf.sunflower.net;

import lombok.Value;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.CryptoContext;
import org.tdf.sunflower.types.Transaction;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// peers cache for peer searching/discovery
class PeersCache {
    private static final int PEER_SCORE = 32;
    private static final int EVIL_SCORE = -(1 << 31);
    private final PeerServerConfig config;
    private final Bucket[] peers = new Bucket[Transaction.ADDRESS_LENGTH * 8];
    private final PeerImpl self;
    Map<PeerImpl, Boolean> bootstraps = new ConcurrentHashMap<>();
    Map<PeerImpl, Boolean> blocked = new ConcurrentHashMap<>();
    Map<PeerImpl, Boolean> trusted = new ConcurrentHashMap<>();

    PeersCache(
            PeerImpl self,
            PeerServerConfig config
    ) {
        this.self = self;
        this.config = config;
    }

    int size() {
        return Stream.of(peers)
                .filter(Objects::nonNull)
                .map(ConcurrentHashMap::size)
                .reduce(0, Integer::sum);
    }

    boolean contains(PeerImpl peer) {
        int idx = self.subTree(peer);
        return peers[idx] != null && peers[idx].containsKey(peer.getID());
    }

    void keep(PeerImpl peer, Channel channel) {
        if (peer.equals(self)) {
            return;
        }
        if (blocked.containsKey(peer)) return;
        int idx = self.subTree(peer);
        if (peers[idx] == null) {
            peers[idx] = new Bucket();
        }

        // if the peer already had been put
        Optional<PeerImpl> o =
                Optional.ofNullable(peers[idx])
                        .map(x -> x.get(peer.getID())).map(x -> x.peer);

        // increase its score
        if (o.isPresent()) {
            PeerImpl p = o.get();
            p.setScore(p.getScore() + PEER_SCORE);
            return;
        }

        peer.setScore(PEER_SCORE);
        PeerChannel newPeerChannel = new PeerChannel(peer, channel);

        if (size() < config.getMaxPeers()) {
            peers[idx].put(peer.getID(), newPeerChannel);
            return;
        }

        // when neighbours is full, check whether some neighbours could be removed
        // 1. the bucket of the new neighbour is empty
        if (peers[idx].size() > 0) {
            channel.close("neighbours is full");
            return;
        }

        // 2. exists some bucket which contains more than one peer
        // find the maximum bucket
        Optional<Bucket> bucket = Stream.of(peers)
                .filter(Objects::nonNull)
                .max(Comparator.comparingInt(Map::size));

        // if the maximum bucket contains less or equals to one element
        // the buckets is ideal, no need to evict
        if (!bucket.isPresent() || bucket.get().size() <= 1) {
            channel.close("neighbours is full");
            return;
        }

        // the conditions above are both filled
        // evict one and add new peer
        bucket.get().keySet()
                .stream().findAny()
                .ifPresent(x -> remove(x, "the new node " + peer + " has more priority than " + x));

        peers[idx].put(peer.getID(), newPeerChannel);
    }

    // remove the peer and close the channel
    void remove(HexBytes peerID, String reason) {
        int idx = self.subTree(peerID.getBytes());
        if (peers[idx] == null) {
            return;
        }
        PeerChannel peerChannel = peers[idx].get(peerID);
        peers[idx].remove(peerID);
        if (peerChannel == null) return;
        peerChannel.channel.close(reason);
    }

    // get limit peers randomly
    List<PeerImpl> getPeers(int limit) {
        List<PeerImpl> res = getPeers().collect(Collectors.toList());
        Random rand = new Random();
        while (res.size() > 0 && res.size() > limit) {
            int idx = Math.abs(rand.nextInt()) % res.size();
            res.remove(idx);
        }
        return res;
    }

    Stream<PeerImpl> getPeers() {
        return Stream.of(peers)
                .filter(Objects::nonNull)
                .flatMap(x -> x.values().stream())
                .map(b -> b.peer);
    }

    void block(PeerImpl peer) {
        // trusted peer will not be blocked
        if (trusted.containsKey(peer)) return;
        // if peer discovery is disabled, bootstrap peer are treat as trusted peer
        if (!config.isEnableDiscovery() && bootstraps.containsKey(peer)) return;
        // if the peer had been blocked before,
        // reset the score of this peer as EVIL_SCORE
        if (blocked.containsKey(peer)) {
            blocked.keySet().stream()
                    .filter(p -> p.equals(peer))
                    .forEach(x -> x.setScore(EVIL_SCORE));
            return;
        }
        // remove the peer and disconnect to it
        remove(peer.getID(), "block the peer " + peer);
        peer.score = EVIL_SCORE;
        blocked.put(peer, true);
    }

    // decrease score of peer
    void half(PeerImpl peer) {
        int idx = self.subTree(peer);
        if (peers[idx] == null) return;
        Optional.ofNullable(
                peers[idx]
                        .get(peer.getID())
        ).map(b -> b.peer).filter(p -> {
            p.score -= p.score < 8 ? p.score : 8;
            p.score /= 2;
            return p.score == 0;
        })
                .ifPresent(x -> remove(x.getID(), " the score of " + x + " is 0"));
    }

    // decrease score of all peer
    void half() {
        List<PeerImpl> toRemoves = Stream.of(peers)
                .filter(Objects::nonNull)
                .flatMap(x -> x.values().stream())
                .filter(b -> {
                    PeerImpl p = b.peer;
                    p.score -= p.score < 8 ? p.score : 8;
                    p.score /= 2;
                    return p.score == 0 || b.channel.isClosed();
                }).map(b -> b.peer).collect(Collectors.toList());
        toRemoves.forEach(x -> remove(x.getID(), " the score of " + x + " is 0"));
        List<PeerImpl> toRestores
                = blocked.keySet().stream()
                .filter(p -> {
                    p.score /= 2;
                    return p.score == 0;
                }).collect(Collectors.toList());
        toRestores.forEach(p -> blocked.remove(p));
    }

    boolean isFull() {
        return size() >= config.getMaxPeers();
    }

    // get all connected channels
    Stream<Channel> getChannels() {
        return Arrays.stream(peers).filter(Objects::nonNull)
                .flatMap(x ->
                        x.values()
                                .stream()
                ).map(bucket -> bucket.channel)
                .filter(Channel::isAlive)
                ;
    }

    // get channel of the peer
    Optional<Channel> getChannel(PeerImpl peer) {
        return getChannel(peer.getID());
    }

    // get channel by peer id
    Optional<Channel> getChannel(HexBytes id) {
        int idx = self.subTree(id.getBytes());
        return Optional.ofNullable(peers[idx])
                .map(x -> x.get(id))
                .map(x -> x.channel)
                .filter(Channel::isAlive)
                ;
    }

    boolean hasBlocked(PeerImpl peer) {
        return blocked.containsKey(peer);
    }

    // helper to avoid generic array
    // kademlia k-bucket
    static class Bucket extends ConcurrentHashMap<HexBytes, PeerChannel> {
    }

    // helper for store peer and channel in a single value in Bucket
    @Value
    static class PeerChannel {
        PeerImpl peer;
        Channel channel;
    }
}
