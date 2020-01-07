package org.tdf.common.util;

import com.google.common.collect.Iterators;
import lombok.NonNull;
import org.tdf.common.types.Chained;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Tree-like object storage
 *
 * @author sal 1564319846@qq.com
 */
public class ChainCache<T extends Chained> extends AbstractSet<T> implements SortedSet<T> {
    public static <T extends Chained> ChainCache<T> of(Collection<T> nodes) {
        Builder<T> builder = builder();
        ChainCache<T> ret = builder.build();
        ret.addAll(nodes);
        return ret;
    }


    // hash -> node
    private Map<byte[], T> nodes = new ByteArrayMap<>();
    // hash -> children hashes
    private Map<byte[], Set<byte[]>> childrenHashes = new ByteArrayMap<>();
    // hash -> parent hash
    private Map<byte[], byte[]> parentHash = new ByteArrayMap<>();

    private TreeSet<T> sorted;

    private int sizeLimit;

    public ChainCache() {
        sorted = new TreeSet<>(comparator);
    }

    // not serializable
    private Comparator<? super T> comparator = (Comparator<T>) (o1, o2) -> {
        if (o1.getHash().equals(o2.getHashPrev())) return -1;
        if (o1.getHashPrev().equals(o2.getHash())) return 1;
        return o1.getHash().compareTo(o2.getHash());
    };

    @lombok.Builder(builderClassName = "Builder")
    public ChainCache(int sizeLimit, Comparator<? super T> comparator) {
        this.sizeLimit = sizeLimit;
        if (comparator != null) this.comparator = comparator;
        sorted = new TreeSet<>(this.comparator);
    }


    public ChainCache<T> withLock() {
        return new ChainCacheWrapper<>(this);
    }

    public Optional<T> get(byte[] hash) {
        return Optional.ofNullable(nodes.get(hash));
    }

    private List<T> getNodes(Collection<byte[]> hashes) {
        Stream<T> stream = new ByteArraySet(hashes).stream()
                .map(k -> nodes.get(k));
        if (comparator != null) stream = stream.sorted(comparator);
        return stream.collect(Collectors.toList());
    }

    public ChainCache<T> clone() {
        Builder<T> builder = builder();
        ChainCache<T> ret = builder.sizeLimit(sizeLimit)
                .comparator(comparator)
                .build();
        ret.nodes = new ByteArrayMap<>(nodes);
        Map<byte[], Set<byte[]>> tmp =
                childrenHashes;
        ret.childrenHashes = new ByteArrayMap<>(tmp);
        ret.parentHash = new ByteArrayMap<>(parentHash);
        ret.sorted = new TreeSet<>(sorted);
        return ret;
    }

    private Set<byte[]> getDescendantsHash(byte[] hash) {
        LinkedList<Set<byte[]>> descendantBlocksHashes = new LinkedList<>();
        descendantBlocksHashes.add(Collections.singleton(hash));
        while (true) {
            Set<byte[]> tmp = descendantBlocksHashes.getLast()
                    .stream().flatMap(x -> childrenHashes.getOrDefault(x, Collections.emptySet()).stream())
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
                .map(k -> nodes.get(k))
                .map(k -> k.getHash().getBytes())
                .map(this::getAncestors)
                .sorted(Comparator.comparingLong(List::size))
                .collect(Collectors.toList());
        Collections.reverse(res);
        return res;
    }

    public void removeDescendants(byte[] hash) {
        getDescendantsHash(hash).forEach(this::removeByHash);
    }

    // leaves not has children
    private Set<byte[]> getLeavesHash() {
        Set<byte[]> res = new ByteArraySet();
        for (byte[] key : nodes.keySet()) {
            if (childrenHashes.getOrDefault(key, Collections.emptySet()).size() == 0) {
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
            T t = nodes.get(key);
            if (t == null) continue;
            if (!nodes.containsKey(t.getHashPrev().getBytes())) {
                res.add(key);
            }
        }
        return res;
    }

    // evict
    private void evict() {
        if (sizeLimit <= 0) {
            return;
        }
        while (size() > sizeLimit) {
            remove(sorted.first());
        }
    }

    public boolean add(@NonNull T node) {
        if (sorted.contains(node)) return false;
        byte[] key = node.getHash().getBytes();
        nodes.put(key, node);
        byte[] prevHash = node.getHashPrev().getBytes();
        childrenHashes.putIfAbsent(prevHash, new ByteArraySet());
        Set<byte[]> s = childrenHashes.get(prevHash);
        parentHash.put(key, prevHash);
        s.add(node.getHash().getBytes());
        childrenHashes.put(prevHash, s);
        sorted.add(node);
        evict();
        return true;
    }
    
    public boolean removeByHash(byte[] hash){
        T n = nodes.get(hash);
        if(n == null) return false;
        return remove(n);
    }

    @Override
    public boolean remove(Object o) {
        T n = (T) o;
        if(!sorted.contains(n)) return false;
        byte[] key = n.getHash().getBytes();
        byte[] k = parentHash.get(key);
        sorted.remove(n);
        nodes.remove(key);
        parentHash.remove(key);
        Set<byte[]> set = childrenHashes.get(k);
        if (set == null) return true;
        set.remove(key);
        if (set.size() > 0) {
            childrenHashes.put(k, set);
        } else {
            childrenHashes.remove(k);
        }
        return true;
    }

    @Override
    public void clear() {
        nodes = new ByteArrayMap<>();
        childrenHashes = new ByteArrayMap<>();
        parentHash = new ByteArrayMap<>();
        sorted = new TreeSet<>(comparator);
    }

    public List<T> popLongestChain() {
        List<List<T>> res = getAllForks();
        if (res.size() == 0) {
            return new ArrayList<>();
        }
        res.sort(Comparator.comparingInt(List::size));
        List<T> longest = res.get(res.size() - 1);
        removeAll(longest);
        return longest;
    }

    public int size() {
        return nodes.size();
    }

    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return sorted.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return Iterators.unmodifiableIterator(sorted.iterator());
    }

    @Override
    public Object[] toArray() {
        return sorted.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return sorted.toArray(a);
    }

    public boolean containsHash(byte[] hash) {
        return nodes.containsKey(hash);
    }


    public List<T> getAncestors(byte[] hash) {
        List<T> res = new ArrayList<>();
        T t = nodes.get(hash);
        while (t != null) {
            res.add(t);
            t = nodes.get(t.getHashPrev().getBytes());
        }
        Collections.reverse(res);
        return res;
    }

    public List<T> getChildren(byte[] hash) {
        Set<byte[]> s = childrenHashes.get(hash);
        if (s == null || s.isEmpty()) return Collections.emptyList();
        return getNodes(s);
    }

    @Override
    public Stream<T> stream() {
        return sorted.stream();
    }

    @Override
    public Spliterator<T> spliterator() {
        return sorted.spliterator();
    }


    @Override
    public Comparator<? super T> comparator() {
        return this.comparator;
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return sorted.subSet(fromElement, toElement);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return sorted.headSet(toElement);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return sorted.tailSet(fromElement);
    }

    @Override
    public T first() {
        return sorted.first();
    }

    @Override
    public T last() {
        return sorted.last();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean[] ret = new boolean[1];
        c.forEach(o -> {
            ret[0] |= remove(o);
        });
        return ret[0];
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }
}
