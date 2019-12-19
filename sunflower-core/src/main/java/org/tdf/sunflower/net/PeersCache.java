package org.tdf.sunflower.net;

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

    static class Bucket {
        Map<PeerImpl, Channel> channels = new ConcurrentHashMap<>();
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
                .map(x -> x.channels.size())
                .reduce(0, Integer::sum);
    }

    boolean has(PeerImpl peer) {
        int idx = self.subTree(peer);
        return peers[idx] != null && peers[idx].channels.containsKey(peer);
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
        Optional<PeerImpl> o = peers[idx].channels.keySet().stream()
                .filter(k -> k.equals(peer)).findAny();

        // increase its score
        if (o.isPresent()) {
            PeerImpl p = o.get();
            p.setScore(p.getScore() + PEER_SCORE);
            peers[idx].channels.put(p, channel);
            return;
        }

        peer.setScore(PEER_SCORE);

        if (size() < config.getMaxPeers()) {
            peers[idx].channels.put(peer, channel);
            return;
        }

        // when neighbours is full, check whether some neighbours could be removed
        // 1. the bucket of the new neighbour is empty
        if (peers[idx].channels.size() > 0) {
            channel.close("neighbours is full");
            return;
        }

        // 2. exists some bucket which contains more than one peer
        Optional<Map<PeerImpl, Channel>> bucket = Stream.of(peers)
                .filter(Objects::nonNull)
                .map(x -> x.channels)
                .max(Comparator.comparingInt(Map::size));

        if (!bucket.isPresent() || bucket.get().size() <= 1) {
            channel.close("neighbours is full");
            return;
        }

        // the conditions above are both filled
        bucket.get().keySet()
                .stream().findAny()
                .ifPresent(x -> remove(x, "the new node " + peer + " has more priority than " + x));

        peers[idx].channels.put(peer, channel);
    }

    // remove the peer and close the channel
    void remove(PeerImpl peer, String reason) {
        int idx = self.subTree(peer);
        if (peers[idx] == null) {
            return;
        }
        Channel ch = peers[idx].channels.get(peer);
        peers[idx].channels.remove(peer);
        if (ch == null) return;
        ch.close(reason);
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
                .flatMap(x -> x.channels.keySet().stream());
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
        peers[idx].channels.keySet()
                .stream()
                .filter(p -> p.equals(peer))
                .findAny()
                .filter(p -> {
                    p.score -= p.score < 8 ? p.score : 8;
                    p.score /= 2;
                    return p.score == 0;
                })
                .ifPresent(x -> remove(x, " the score of " + x + " is 0"));
    }

    // decrease score of all peer
    void half() {
        List<PeerImpl> toRemoves = Stream.of(peers).filter(Objects::nonNull)
                .flatMap(x -> x.channels.keySet().stream())
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
                .flatMap(x -> x.channels.values().stream());
    }

    // get channel of the peer
    Optional<Channel> getChannel(PeerImpl peer) {
        int idx = self.subTree(peer);
        if (peers[idx] == null) return Optional.empty();
        return Optional.ofNullable(peers[idx].channels.get(peer));
    }

    // get channel by peer id
    Optional<Channel> getChannel(HexBytes id) {
        int idx = self.subTree(id.getBytes());
        if (peers[idx] == null) return Optional.empty();
        return peers[idx].channels.keySet().stream()
                .filter(p -> p.getID().equals(id))
                .findAny()
                .map(p -> peers[idx].channels.get(p))
                ;
    }

    boolean hasBlocked(PeerImpl peer) {
        return blocked.containsKey(peer);
    }
}
