package org.tdf.common.trie;

import org.tdf.common.serialize.Codec;
import org.tdf.common.store.Store;
import org.tdf.rlp.RLPElement;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

public interface Trie<K, V> extends Store<K, V> {
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
     * @param keys keys to prove be absent or valid
     * @return merkle path of key
     */
    RLPElement getProof(Collection<? extends K> keys);

    default RLPElement getProof(K key){
        return getProof(Collections.singleton(key));
    }

    /**
     * Get a minimal trie revert from merkle path for verifying;
     * the trie should have the equality root hash
     * @param proof
     * @return Trie represented by merkle path
     */
    Trie<K, V> revertToProof(RLPElement proof);

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
}
