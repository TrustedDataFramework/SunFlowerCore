package org.tdf.common.util;

import lombok.Getter;
import lombok.NonNull;
import org.tdf.common.store.ByteArrayMapStore;
import org.tdf.common.store.Store;
import org.tdf.common.store.StoreUtil;
import org.tdf.common.types.Chained;
import org.tdf.common.types.Cloneable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Tree-like object storage
 *
 * @author sal 1564319846@qq.com
 */
public class ChainCache<T extends Chained> implements Cloneable<ChainCache<T>> {
    private static final String NODE_PREFIX = "node";
    private static final String CHILDREN_HASH_PREFIX = "children";
    private static final String PARENT_HASH_PREFIX = "parent";

    // hash -> node
    @Getter
    protected Store<byte[], T> nodes;
    // hash -> children hashes
    @Getter
    protected Store<byte[], Set<byte[]>> childrenHashes;
    // hash -> parent hash
    @Getter
    protected Store<byte[], byte[]> parentHash;

    protected int sizeLimit;

    // not serializable
    protected Comparator<? super T> comparator;

    public ChainCache<T> withComparator(Comparator<? super T> comparator) {
        this.comparator = comparator;
        return this;
    }

    // lru
    public ChainCache(int sizeLimit, Comparator<? super T> comparator) {
        this();
        this.sizeLimit = sizeLimit;
        this.comparator = comparator;
    }

    public ChainCache() {
        this.nodes = new ByteArrayMapStore<>();
        this.childrenHashes = new ByteArrayMapStore<>();
        this.parentHash = new ByteArrayMapStore<>();
    }

    public ChainCache(T node) {
        this();
        put(node);
    }

    public ChainCache(Collection<? extends T> nodes) {
        this();
        put(nodes);
    }

    public ChainCache<T> withLock(){
        return new ChainCacheWrapper<>(this);
    }

    public Optional<T> get(byte[] hash) {
        return nodes.get(hash);
    }

    private List<T> getNodes(Collection<byte[]> hashes) {
        Stream<T> stream = new ByteArraySet(hashes).stream()
                .map(k -> nodes.get(k).orElseThrow(() -> ExceptionUtil.keyNotFound(HexBytes.encode(k))));
        if (comparator != null) stream = stream.sorted(comparator);
        return stream.collect(Collectors.toList());
    }

    public ChainCache<T> clone() {
        ChainCache<T> copied = new ChainCache<>();
        copied.nodes = new ByteArrayMapStore<>(nodes);
        Map<byte[], Set<byte[]>> tmp =
                StoreUtil.storeToMap(childrenHashes, x -> x, ByteArraySet::new);
        copied.childrenHashes = new ByteArrayMapStore<>(tmp);
        copied.parentHash = new ByteArrayMapStore<>(parentHash);
        return copied;
    }

    private Set<byte[]> getDescendantsHash(byte[] hash) {
        LinkedList<Set<byte[]>> descendantBlocksHashes = new LinkedList<>();
        descendantBlocksHashes.add(Collections.singleton(hash));
        while (true) {
            Set<byte[]> tmp = descendantBlocksHashes.getLast()
                    .stream().flatMap(x -> childrenHashes.get(x).orElse(new ByteArraySet()).stream())
                    .collect(Collectors.toSet());
            if (tmp.size() == 0) {
                break;
            }
            descendantBlocksHashes.add(tmp);
        }
        return descendantBlocksHashes.stream().flatMap(Collection::stream).collect(Collectors.toSet());
    }

    public List<T> getDescendants(byte[] hash) {
        return getNodes(getDescendantsHash(hash));
    }

    public List<List<T>> getAllForks() {
        List<List<T>> res = getLeavesHash().stream()
                .map(k -> nodes.get(k).orElseThrow(() -> ExceptionUtil.keyNotFound(HexBytes.encode(k))))
                .map(k -> k.getHash().getBytes())
                .map(this::getAncestors)
                .sorted(Comparator.comparingLong(List::size))
                .collect(Collectors.toList());
        Collections.reverse(res);
        return res;
    }

    public void removeDescendants(byte[] hash) {
        getDescendantsHash(hash).forEach(this::remove);
    }

    public void remove(byte[] key) {
        Optional<byte[]> o = parentHash.get(key);
        if (!o.isPresent()) return;
        nodes.remove(key);
        parentHash.remove(key);
        Optional<Set<byte[]>> set = childrenHashes.get(o.get());
        if (!set.isPresent()) return;
        set.get().remove(key);
        if (set.get().size() > 0) {
            childrenHashes.put(o.get(), set.get());
        } else {
            childrenHashes.remove(o.get());
        }
    }

    public void remove(Collection<byte[]> nodes) {
        for (byte[] node : nodes) {
            remove(node);
        }
    }

    // leaves not has children
    private Set<byte[]> getLeavesHash() {
        Set<byte[]> res = new ByteArraySet();
        for (byte[] key : nodes.keySet()) {
            if (childrenHashes.get(key).map(x -> x.size() == 0).orElse(true)) {
                res.add(key);
            }
        }
        return res;
    }


    public List<T> getLeaves() {
        return getNodes(getLeavesHash());
    }

    public List<T> getInitials() {
        return getNodes(getInitialsHash());
    }

    // initials not has parent
    private Set<byte[]> getInitialsHash() {
        Set<byte[]> res = new ByteArraySet();
        for (byte[] key : nodes.keySet()) {
            Optional<T> o = nodes.get(key);
            if (!o.isPresent()) continue;
            if (!nodes.containsKey(o.get().getHashPrev().getBytes())) {
                res.add(key);
            }
        }
        return res;
    }

    // evict
    private void evict() {
        if (comparator == null || sizeLimit <= 0) {
            return;
        }
        long toRemove = size() - sizeLimit;
        toRemove = toRemove > 0 ? toRemove : 0;
        this.nodes.values();

        List<T> ss = this.nodes.values()
                .stream().sorted(comparator)
                .limit(toRemove)
                .collect(Collectors.toList());
        ss.stream().map(n -> n.getHash().getBytes())
                .forEach(this::remove);
    }

    public void putIfAbsent(@NonNull T node) {
        if (nodes.containsKey(node.getHash().getBytes())) return;
        put(node);
    }

    public void put(@NonNull T node) {
        byte[] key = node.getHash().getBytes();
        nodes.put(key, node);
        byte[] prevHash = node.getHashPrev().getBytes();
        childrenHashes.putIfAbsent(prevHash, new ByteArraySet());
        Set<byte[]> s = childrenHashes.get(prevHash)
                .orElseThrow(() -> ExceptionUtil.keyNotFound(HexBytes.encode(prevHash)));
        parentHash.put(key, prevHash);
        s.add(node.getHash().getBytes());
        childrenHashes.put(prevHash, s);
        evict();
    }

    public void put(@NonNull Collection<? extends T> nodes) {
        for (T b : nodes) {
            put(b);
        }
    }

    public List<T> getAll() {
        return getNodes(nodes.keySet());
    }

    public List<T> popLongestChain() {
        List<List<T>> res = getAllForks();
        if (res.size() == 0) {
            return new ArrayList<>();
        }
        res.sort(Comparator.comparingInt(List::size));
        List<T> longest = res.get(res.size() - 1);
        remove(longest.stream().map(n -> n.getHash().getBytes()).collect(Collectors.toList()));
        return longest;
    }

    public int size() {
        return nodes.size();
    }

    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    public boolean contains(byte[] hash) {
        return nodes.containsKey(hash);
    }


    public List<T> getAncestors(byte[] hash) {
        List<T> res = new ArrayList<>();
        Optional<T> o = nodes.get(hash);
        while (o.isPresent()) {
            res.add(o.get());
            o = nodes.get(o.get().getHashPrev().getBytes());
        }
        Collections.reverse(res);
        return res;
    }

    public List<T> getChildren(byte[] hash) {
        Optional<Set<byte[]>> o = childrenHashes.get(hash);
        if (!o.isPresent()) return new ArrayList<>();
        return getNodes(o.get());
    }
}
