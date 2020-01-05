package org.tdf.common.trie;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.tdf.common.serialize.Codec;
import org.tdf.common.store.ReadOnlyStore;
import org.tdf.common.store.Store;
import org.tdf.common.util.FastByteComparisons;
import org.tdf.rlp.RLPItem;

import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;


// enhanced radix tree
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TrieImpl<K, V> implements Trie<K, V> {
    @Getter
    private final byte[] nullHash;
    Function<byte[], byte[]> function;
    Store<byte[], byte[]> store;
    Codec<K, byte[]> kCodec;
    Codec<V, byte[]> vCodec;
    private Node root;

    public static <K, V> TrieImpl<K, V> newInstance(Function<byte[], byte[]> hashFunction,
                                                    Store<byte[], byte[]> store,
                                                    Codec<K, byte[]> kCodec,
                                                    Codec<V, byte[]> vCodec
    ) {
        return new TrieImpl<>(
                hashFunction.apply(RLPItem.NULL.getEncoded()),
                hashFunction,
                store,
                kCodec,
                vCodec,
                null
        );
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
        root.insert(TrieKey.fromNormal(key), value, store);
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
        root = root.delete(TrieKey.fromNormal(data), store);
    }

    @Override
    public boolean containsKey(@NonNull K k) {
        return get(k).isPresent();
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
        if (root == null) return nullHash;
        if (!root.isDirty()) return root.getHash();
        byte[] hash = this.root.commit(function, store, true).asBytes();
        if (root.isDirty() || root.getHash() == null)
            throw new RuntimeException("unexpected error: still dirty after commit");
        return hash;
    }

    @Override
    public void flush() {
        store.flush();
    }

    @Override
    public TrieImpl<K, V> revert(@NonNull byte[] rootHash, Store<byte[], byte[]> store) {
        if (FastByteComparisons.equal(rootHash, nullHash))
            return new TrieImpl<>(nullHash, function, store,
                    kCodec, vCodec,
                    Node.fromRootHash(rootHash, new ReadOnlyStore<>(store))
            );
        if (!store.containsKey(rootHash)) throw new RuntimeException("rollback failed, root hash not exists");
        return new TrieImpl<>(
                nullHash,
                function,
                store, kCodec, vCodec,
                Node.fromRootHash(rootHash, new ReadOnlyStore<>(store))
        );
    }

    @Override
    public void traverse(BiConsumer<TrieKey, Node> action) {
        commit();
        if (root == null) return;
        root.traverse(TrieKey.EMPTY, action);
    }

    @Override
    public Set<byte[]> dump() {
        Dump dump = new Dump();
        traverse(dump);
        return dump.getKeys();
    }

    @Override
    public byte[] getRootHash() throws RuntimeException {
        if (root == null) return nullHash;
        if (root.isDirty() || root.getHash() == null)
            throw new RuntimeException("the trie is dirty or root hash is null");
        return root.getHash();
    }

    @Override
    public boolean isDirty() {
        return root != null && root.isDirty();
    }

    @Override
    public Trie<K, V> revert(byte[] rootHash) throws RuntimeException {
        return revert(rootHash, store);
    }

    @Override
    public Trie<K, V> revert() {
        return new TrieImpl<>(
                nullHash,
                function,
                store, kCodec, vCodec,
                null
        );
    }

    @Override
    public void forEach(BiConsumer<K, V> consumer) {
        traverse((k, n) -> {
            if (n.getType() != Node.Type.EXTENSION && n.getValue() != null) {
                consumer.accept(kCodec.getDecoder().apply(k.toNormal()), vCodec.getDecoder().apply(n.getValue()));
            }
        });
    }
}
