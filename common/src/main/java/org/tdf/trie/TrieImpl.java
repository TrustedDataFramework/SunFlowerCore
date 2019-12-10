package org.tdf.trie;

import com.google.common.base.Functions;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
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
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TrieImpl<K, V> implements Trie<K, V> {
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    private Node root;

    HashFunction function;

    CachedStore<byte[]> cache;

    Codec<K, byte[]> kCodec;

    Codec<V, byte[]> vCodec;

    private TrieImpl() {
    }

    public TrieImpl(HashFunction function, Store<byte[], byte[]> store, Codec<K, byte[]> kCodec, Codec<V, byte[]> vCodec) {
        this.function = function;
        this.cache = new CachedStore<>(store);
        this.kCodec = kCodec;
        this.vCodec = vCodec;
    }

    @Override
    public Optional<V> get(@NonNull K k) {
        if (root == null) return Optional.empty();
        byte[] data = kCodec.getEncoder().apply(k);
        if (data == null || data.length == 0) return Optional.empty();
        return Optional.ofNullable(root.get(TrieKey.fromNormal(data))).map(vCodec.getDecoder());
    }

    public void put(@NonNull K k, @NonNull V val) {
        putBytes(kCodec.getEncoder().apply(k), vCodec.getEncoder().apply(val));
    }

    private void putBytes(byte[] bytes, byte[] bytes2) {
        if (bytes == null || bytes.length == 0) throw new IllegalArgumentException("key cannot be null");
        if (bytes2 == null || bytes2.length == 0) {
            removeBytes(bytes);
            return;
        }
        if (root == null) {
            root = Node.newLeaf(TrieKey.fromNormal(bytes), bytes2);
            return;
        }
        root.insert(TrieKey.fromNormal(bytes), bytes2, cache);
    }

    @Override
    public void putIfAbsent(@NonNull K k, @NonNull V val) {
        if (containsKey(k)) return;
        put(k, val);
    }

    @Override
    public void remove(K k) {
        if (root == null) return;
        byte[] data = kCodec.getEncoder().apply(k);
        if(data == null || data.length == 0) return;
        root = root.delete(TrieKey.fromNormal(data), cache);
    }

    private void removeBytes(byte[] data) {
        if (root == null) return;
        if(data == null || data.length == 0) return;
        root = root.delete(TrieKey.fromNormal(data), cache);
    }

    @Override
    public Set<K> keySet() {
        if (root == null) return Collections.emptySet();
        ScanKeySet action = new ScanKeySet();
        root.traverse(EMPTY, action);
        return action.getBytes().stream()
                .map(kCodec.getDecoder())
                .collect(Collectors.toSet());
    }

    @Override
    public Collection<V> values() {
        if (root == null) return Collections.emptySet();
        ScanValues action = new ScanValues();
        root.traverse(EMPTY, action);
        return action.getBytes().stream()
                .map(vCodec.getDecoder())
                .collect(Collectors.toList());
    }

    @Override
    public boolean containsKey(K k) {
        if (root == null) return false;
        return root.get(TrieKey.fromNormal(kCodec.getEncoder().apply(k))) != null;
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
    public TrieImpl<K, V> createSnapshot() {
        commit();
        CachedStore<byte[]> cloned = cache.clone();
        return new TrieImpl<>(
                Node.fromRootHash(getRootHash(), new ReadOnlyStore<>(cloned)),
                function, cloned, kCodec, vCodec
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
