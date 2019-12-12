package org.tdf.trie;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import org.tdf.rlp.RLPItem;
import org.tdf.serialize.Codec;
import org.tdf.store.MemoryCachedStore;
import org.tdf.store.ReadOnlyStore;
import org.tdf.store.Store;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


// enhanced radix tree
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TrieImpl<K, V> implements Trie<K, V> {
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    private Node root;

    Function<byte[], byte[]> function;

    MemoryCachedStore<byte[]> cache;

    Codec<K, byte[]> kCodec;

    Codec<V, byte[]> vCodec;

    private TrieImpl() {
    }

    public TrieImpl(Function<byte[], byte[]> hashFunction,
                    Store<byte[], byte[]> store,
                    Codec<K, byte[]> kCodec,
                    Codec<V, byte[]> vCodec
    ) {
        this.function = hashFunction;
        this.cache = new MemoryCachedStore<>(store);
        this.kCodec = kCodec;
        this.vCodec = vCodec;
    }

    @Override
    public Optional<V> get(@NonNull K k) {
        byte[] data = kCodec.getEncoder().apply(k);
        if (data == null || data.length == 0) throw new IllegalArgumentException("key cannot be null");
        if (root == null) return Optional.empty();
        return Optional.ofNullable(root.get(TrieKey.fromNormal(data))).map(vCodec.getDecoder());
    }

    @Override
    public void put(@NonNull K k, @NonNull V val) {
        putBytes(kCodec.getEncoder().apply(k), vCodec.getEncoder().apply(val));
    }

    private void putBytes(byte[] key, byte[] value) {
        if (key == null || key.length == 0) throw new IllegalArgumentException("key cannot be null");
        if (value == null || value.length == 0) {
            removeBytes(key);
            return;
        }
        if (root == null) {
            root = Node.newLeaf(TrieKey.fromNormal(key), value);
            return;
        }
        root.insert(TrieKey.fromNormal(key), value, cache);
    }

    @Override
    public void putIfAbsent(@NonNull K k, @NonNull V val) {
        if (containsKey(k)) return;
        put(k, val);
    }

    @Override
    public void remove(@NonNull K k) {
        byte[] data = kCodec.getEncoder().apply(k);
        removeBytes(data);
    }

    private void removeBytes(byte[] data) {
        if (data == null || data.length == 0) throw new IllegalArgumentException("key cannot be null");
        if (root == null) return;
        root = root.delete(TrieKey.fromNormal(data), cache);
    }

    @Override
    public Set<K> keySet() {
        if (root == null) return Collections.emptySet();
        ScanKeySet action = new ScanKeySet();
        root.traverse(TrieKey.EMPTY, action);
        if (kCodec.equals(Codec.identity())) return (Set<K>) action.getBytes();
        return action.getBytes().stream()
                .map(kCodec.getDecoder())
                .collect(Collectors.toSet());
    }

    @Override
    public Collection<V> values() {
        if (root == null) return Collections.emptySet();
        ScanValues action = new ScanValues();
        root.traverse(TrieKey.EMPTY, action);
        if (vCodec.equals(Codec.identity())) return (Set<V>) action.getBytes();
        return action.getBytes().stream()
                .map(vCodec.getDecoder())
                .collect(Collectors.toSet());
    }

    @Override
    public boolean containsKey(@NonNull K k) {
        return get(k).isPresent();
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


    public byte[] commit() {
        if (root == null) return function.apply(RLPItem.NULL.getEncoded());
        if(!root.isDirty()) return root.getHash();
        byte[] hash = this.root.commit(function, cache, true).getAsItem().get();
        if(root.isDirty() || root.getHash() == null) throw new RuntimeException("unexpected error: dirty after commit");
        return hash;
    }

    @Override
    public void flush() {
        commit();
        cache.flush();
    }

    @Override
    public TrieImpl<K, V> moveTo(byte[] rootHash, Store<byte[], byte[]> store) {
        return new TrieImpl<>(
                Node.fromRootHash(rootHash, new ReadOnlyStore<>(store)),
                function, new MemoryCachedStore<>(store), kCodec, vCodec
        );
    }

    // for tests only
    TrieImpl<K, V> createSnapshot() {
        commit();
        MemoryCachedStore<byte[]> cloned = cache.clone();
        return new TrieImpl<>(
                Node.fromRootHash(commit(), new ReadOnlyStore<>(cloned)),
                function, cloned, kCodec, vCodec
        );
    }

    @Override
    public void traverse(ScannerAction action) {
        commit();
        root.traverse(TrieKey.EMPTY, action);
    }
}
