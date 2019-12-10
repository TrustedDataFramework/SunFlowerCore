package org.tdf.trie;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.tdf.serialize.Codec;
import org.tdf.serialize.RLPItem;
import org.tdf.store.CachedStore;
import org.tdf.store.ReadOnlyStore;
import org.tdf.store.Store;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.tdf.trie.TrieKey.EMPTY;

// enhanced radix tree
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class TrieImpl<V> implements Trie<V> {
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    private Node root;

    HashFunction function;

    CachedStore<byte[]> cache;

    Codec<V, byte[]> codec;

    private TrieImpl() {
    }

    public TrieImpl(HashFunction function, Store<byte[], byte[]> store, Codec<V, byte[]> codec) {
        this.function = function;
        this.cache = new CachedStore<>(store);
        this.codec = codec;
    }

    @Override
    public Optional<V> get(byte[] bytes) {
        if (root == null) return Optional.empty();
        return Optional.ofNullable(root.get(TrieKey.fromNormal(bytes))).map(codec.getDecoder());
    }

    public void put(byte[] bytes, V val){
        putBytes(bytes, codec.getEncoder().apply(val));
    }

    private void putBytes(byte[] bytes, byte[] bytes2) {
        if (bytes == null || bytes.length == 0) throw new IllegalArgumentException("key cannot be null");
        if (bytes2 == null || bytes2.length == 0) {
            remove(bytes);
            return;
        }
        if (root == null) {
            root = Node.newLeaf(TrieKey.fromNormal(bytes), bytes2);
            return;
        }
        root.insert(TrieKey.fromNormal(bytes), bytes2, cache);
    }

    @Override
    public void putIfAbsent(byte[] bytes, V val) {
        if (root != null && root.get(TrieKey.fromNormal(bytes)) != null) return;
        put(bytes, val);
    }

    @Override
    public void remove(byte[] bytes) {
        if (root == null) return;
        root = root.delete(TrieKey.fromNormal(bytes), cache);
    }

    @Override
    public Set<byte[]> keySet() {
        if (root == null) return Collections.emptySet();
        ScanKeySet action = new ScanKeySet();
        root.traverse(EMPTY, action);
        return action.getBytes();
    }

    @Override
    public Collection<V> values() {
        if (root == null) return Collections.emptySet();
        ScanValues action = new ScanValues();
        root.traverse(EMPTY, action);
        return action.getBytes().stream()
                .map(codec.getDecoder())
                .collect(Collectors.toList());
    }

    @Override
    public boolean containsKey(byte[] bytes) {
        if (root == null) return false;
        return root.get(TrieKey.fromNormal(bytes)) != null;
    }

    @Override
    public int size() {
        return keySet().size();
    }

    @Override
    public boolean isEmpty() {
        return root != null;
    }

    @Override
    public void clear() {
        root = null;
    }


    public byte[] getRootHash() {
        commit();
        if (root == null) return function.apply(RLPItem.NULL.getEncoded());
        return root.getHash();
    }

    private void commit() {
        if (root == null || !isDirty()) return;
        this.root.encodeAndCommit(function, cache, true, true);
    }

    @Override
    public TrieImpl<V> createSnapshot() {
        commit();
        CachedStore<byte[]> cloned = cache.clone();
        return new TrieImpl<>(
                Node.fromRootHash(getRootHash(), new ReadOnlyStore<>(cloned)),
                function, cloned, codec
        );
    }

    @Override
    public boolean flush() {
        commit();
        return this.cache.flush();
    }

    boolean isDirty() {
        return root != null && root.isDirty();
    }
}
