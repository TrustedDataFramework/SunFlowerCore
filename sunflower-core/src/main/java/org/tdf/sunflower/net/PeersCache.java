package org.tdf.sunflower.net;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.tdf.common.util.HexBytes;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// peers cache for peer searching/discovery
class PeersCache {
    private static final int PEER_SCORE = 32;
    private static final int EVIL_SCORE = -(1 << 31);
    private PeerServerConfig config;

    // helper to avoid generic array
    // kademlia k-bucket
    static class Bucket extends ConcurrentHashMap<PeerImpl, PeerChannel>{}

    // helper for store peer and channel in a single value in Bucket
    @AllArgsConstructor
    @Builder
    @NoArgsConstructor
    static class PeerChannel{
        PeerImpl peer;
        Channel channel;
    }

    private Bucket[] peers = new Bucket[256];

    Map<PeerImpl, Boolean> bootstraps = new ConcurrentHashMap<>();

    Map<PeerImpl, Boolean> blocked = new ConcurrentHashMap<>();

    Map<PeerImpl, Boolean> trusted = new ConcurrentHashMap<>();

    private PeerImpl self;

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
        return peers[idx] != null && peers[idx].containsKey(peer);
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
                .map(x -> x.get(peer)).map(x -> x.peer);

        // increase its score
        if (o.isPresent()) {
            PeerImpl p = o.get();
            p.setScore(p.getScore() + PEER_SCORE);
            return;
        }

        peer.setScore(PEER_SCORE);
        PeerChannel newPeerChannel = PeerChannel.builder()
                .peer(peer)
                .channel(channel)
                .build();

        if (size() < config.getMaxPeers()) {
            peers[idx].put(newPeerChannel.peer, newPeerChannel);
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

        peers[idx].put(newPeerChannel.peer, newPeerChannel);
    }

    // remove the peer and close the channel
    void remove(PeerImpl peer, String reason) {
        int idx = self.subTree(peer);
        if (peers[idx] == null) {
            return;
        }
        PeerChannel peerChannel = peers[idx].get(peer);
        peers[idx].remove(peer);
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
        remove(peer, "block the peer " + peer);
        peer.score = EVIL_SCORE;
        blocked.put(peer, true);
    }

    // decrease score of peer
    void half(PeerImpl peer) {
        int idx = self.subTree(peer);
        if (peers[idx] == null) return;
        Optional.ofNullable(
                peers[idx]
                .get(peer)
        ).map(b -> b.peer).filter(p -> {
            p.score -= p.score < 8 ? p.score : 8;
            p.score /= 2;
            return p.score == 0;
        })
        .ifPresent(x -> remove(x, " the score of " + x + " is 0"));
    }

    // decrease score of all peer
    void half() {
        List<PeerImpl> toRemoves = Stream.of(peers)
                .filter(Objects::nonNull)
                .flatMap(x -> x.values().stream())
                .map(b -> b.peer)
                .filter(p -> {
                    p.score -= p.score < 8 ? p.score : 8;
                    p.score /= 2;
                    return p.score == 0;
                }).collect(Collectors.toList());
        toRemoves.forEach(x -> remove(x, " the score of " + x + " is 0"));
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

                ).map(bucket -> bucket.channel);
    }

    // get channel of the peer
    Optional<Channel> getChannel(PeerImpl peer) {
        int idx = self.subTree(peer);
        return Optional.ofNullable(peers[idx])
                .map(x -> x.get(peer))
                .map(x -> x.channel);
    }

    // get channel by peer id
    Optional<Channel> getChannel(HexBytes id) {
        return getChannel(PeerImpl
                .builder()
                .ID(id)
                .build()
        );
    }

    boolean hasBlocked(PeerImpl peer) {
        return blocked.containsKey(peer);
    }
}
