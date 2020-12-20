package org.tdf.common.trie;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.tdf.common.serialize.Codec;
import org.tdf.common.store.ReadOnlyStore;
import org.tdf.common.store.Store;
import org.tdf.common.util.ByteArrayMap;
import org.tdf.common.util.FastByteComparisons;
import org.tdf.rlp.RLPItem;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;


// enhanced radix tree
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TrieImpl<K, V> extends AbstractTrie<K, V> {
    @Getter
    private final byte[] nullHash;
    HashFunction function;

    @Getter
    Store<byte[], byte[]> store;

    Codec<K, byte[]> kCodec;
    Codec<V, byte[]> vCodec;
    private Node root;

    static <K, V> TrieImpl<K, V> newInstance(
            @NonNull Function<byte[], byte[]> hashFunction,
            @NonNull Store<byte[], byte[]> store,
            @NonNull Codec<K, byte[]> keyCodec,
            @NonNull Codec<V, byte[]> valueCodec
    ) {

        return new TrieImpl<>(
                hashFunction.apply(RLPItem.NULL.getEncoded()),
                new HashFunction(hashFunction),
                store,
                keyCodec,
                valueCodec,
                null
        );
    }

    Codec<K, byte[]> getKCodec() {
        return kCodec;
    }

    Codec<V, byte[]> getVCodec() {
        return vCodec;
    }

    Optional<V> getFromBytes(byte[] data) {
        if (data == null || data.length == 0) throw new IllegalArgumentException("key cannot be null");
        if (root == null) return Optional.empty();
        return Optional.ofNullable(root.get(TrieKey.fromNormal(data))).map(vCodec.getDecoder());
    }

    void putBytes(byte[] key, byte[] value) {
        if (key == null || key.length == 0) throw new IllegalArgumentException("key cannot be null");
        if (value == null || value.length == 0) {
            removeBytes(key);
            return;
        }
        if (root == null) {
            root = Node.newLeaf(TrieKey.fromNormal(key), value, function);
            return;
        }
        root.insert(TrieKey.fromNormal(key), value, store);
    }

    void removeBytes(byte[] data) {
        if (data == null || data.length == 0) throw new IllegalArgumentException("key cannot be null");
        if (root == null) return;
        root = root.delete(TrieKey.fromNormal(data), store);
    }

    @Override
    public boolean isEmpty() {
        return root == null;
    }

    @Override
    public void clear() {
        root = null;
    }


    public byte[] commit() {
        if (root == null) return nullHash;
        if (!root.isDirty()) return root.getHash();
        byte[] hash = this.root.commit(store, true).asBytes();
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
                    kCodec, vCodec, null);
        if (!store.containsKey(rootHash)) throw new RuntimeException("rollback failed, root hash not exists");
        return new TrieImpl<>(
                nullHash,
                function,
                store, kCodec, vCodec,
                Node.fromRootHash(rootHash, ReadOnlyStore.of(store), function)
        );
    }

    public void traverseTrie(BiFunction<TrieKey, Node, Boolean> action) {
        if (root == null) return;
        root.traverse(TrieKey.EMPTY, action);
    }

    @Override
    public Set<byte[]> dumpKeys() {
        if (isDirty()) throw new UnsupportedOperationException();
        DumpKeys dump = new DumpKeys();
        traverseTrie(dump);
        return dump.getKeys();
    }

    @Override
    public Map<byte[], byte[]> dump() {
        if (isDirty()) throw new UnsupportedOperationException();
        Dump dump = new Dump();
        traverseTrie(dump);
        return dump.getPairs();
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
    public TrieImpl<K, V> revert(byte[] rootHash) throws RuntimeException {
        return revert(rootHash, store);
    }

    @Override
    public TrieImpl<K, V> revert() {
        return new TrieImpl<>(
                nullHash,
                function,
                store, kCodec, vCodec,
                null
        );
    }

    @Override
    void traverseInternal(BiFunction<byte[], byte[], Boolean> traverser) {
        traverseTrie((k, n) -> {
            if (n.getType() != Node.Type.EXTENSION && n.getValue() != null) {
                return traverser.apply(k.toNormal(), n.getValue());
            }
            return true;
        });
    }


    @Override
    public boolean isTrap(V v) {
        byte[] encoded = vCodec.getEncoder().apply(v);
        return encoded == null || encoded.length == 0;
    }

    public Map<byte[], byte[]> getProofInternal(byte[] key) {
        return root == null ?
                Collections.emptyMap() :
                root.getProof(
                        TrieKey.fromNormal(key),
                        new ByteArrayMap<>()
                );
    }
}
