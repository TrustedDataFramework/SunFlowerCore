package org.tdf.common.trie;

import org.tdf.common.serialize.Codec;
import org.tdf.common.store.MemoryDatabaseStore;
import org.tdf.common.store.NoDeleteStore;
import org.tdf.common.store.Store;
import org.tdf.common.util.ByteArrayMap;
import org.tdf.common.util.ByteArraySet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Trie<K, V> extends Store<K, V> {
    Store<byte[], byte[]> getStore();

    static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    /**
     * rollback to a previous trie
     *
     * @param rootHash previous trie's root hash
     * @param store    the underlying storage of trie
     * @return trie with root hash
     * @throws RuntimeException if the root hash not found in the store or rollback failed
     */
    Trie<K, V> revert(byte[] rootHash, Store<byte[], byte[]> store) throws RuntimeException;

    /**
     * rollback to a previous trie
     *
     * @param rootHash previous trie's root hash
     * @return trie with root hash
     * @throws RuntimeException if the root hash not found in the store or roll back failed
     */
    Trie<K, V> revert(byte[] rootHash) throws RuntimeException;

    /**
     * rollback to an empty trie
     *
     * @return an empty trie
     */
    Trie<K, V> revert();

    /**
     * commit modifications and build a new trie
     *
     * @return root hash of new trie
     */
    byte[] commit();

    /**
     * dump keys this trie
     *
     * @return minimal key value pairs to store this trie
     * @throws RuntimeException if the trie is both non-null and dirty
     */
    Set<byte[]> dumpKeys();

    /**
     * dump this trie
     *
     * @return minimal key value pairs to store this trie
     * @throws RuntimeException if the trie is both non-null and dirty
     */
    Map<byte[], byte[]> dump();

    /**
     * get root hash of a non-dirty tree, return null hash if the trie is null
     *
     * @return trie's root hash
     * @throws RuntimeException if this trie is dirty
     */
    byte[] getRootHash() throws RuntimeException;

    /**
     * get root hash of an empty trie
     *
     * @return root hash of an empty trie
     */
    byte[] getNullHash();

    /**
     * trie is both non-null and has uncommitted modifications
     *
     * @return true when trie is both non-null and dirty
     */
    boolean isDirty();

    /**
     * Get merkle path of key as proof, commonly used for SPV
     * Merkle proof are used to decide upon the following factors
     * If the key belongs in the merkle Trie
     * To concisely prove the validity of data being part of a dataset without storing the whole data set
     *
     * @param k key to prove be absent or valid
     * @return merkle path of key
     */
    Map<byte[], byte[]> getProof(K k);

    /**
     * get merkle proof batched
     *
     * @param keys keys to included in the proof
     * @return merkle proof
     */
    default Map<byte[], byte[]> getProof(Collection<? extends K> keys) {
        Map<byte[], byte[]> ret = new ByteArrayMap<>();
        for (K key : keys) {
            ret.putAll(getProof(key));
        }
        return ret;
    }

    class Builder<K, V> {
        private Function<byte[], byte[]> hashFunction;
        private Store<byte[], byte[]> store;
        private Codec<K, byte[]> keyCodec;
        private Codec<V, byte[]> valueCodec;

        public Builder<K, V> hashFunction(Function<byte[], byte[]> hashFunction) {
            this.hashFunction = hashFunction;
            return this;
        }

        public Builder<K, V> store(Store<byte[], byte[]> store) {
            this.store = store;
            return this;
        }

        public Builder<K, V> keyCodec(Codec<K, byte[]> keyCodec) {
            this.keyCodec = keyCodec;
            return this;
        }

        public Builder<K, V> valueCodec(Codec<V, byte[]> valueCodec) {
            this.valueCodec = valueCodec;
            return this;
        }

        public Trie<K, V> build() {
            return TrieImpl.newInstance(hashFunction, store, keyCodec, valueCodec);
        }
    }

    default void prune(Collection<? extends byte[]> excludedRoots) {
        prune(excludedRoots, getStore());
    }

    default void prune(Collection<? extends byte[]> excludedRoots, Store<byte[], byte[]> db) {
        Set<byte[]> excludes = new ByteArraySet();
        for (byte[] root : excludedRoots) {
            excludes.addAll(revert(root).dumpKeys());
        }
        Consumer<byte[]> fn = k -> {
            if (!excludes.contains(k))
                db.remove(k);
        };
        boolean isMem = db instanceof MemoryDatabaseStore ||
                (
                        db instanceof NoDeleteStore &&
                                ((NoDeleteStore) db).getDelegate() instanceof MemoryDatabaseStore
                );
        if (isMem) {
            new ArrayList<>(db.keySet()).forEach(fn);
        } else {
            db.stream().forEach(e -> fn.accept(e.getKey()));
        }
    }
}
