package org.tdf.common.trie;

import org.tdf.common.serialize.Codec;
import org.tdf.common.store.Store;

import java.util.Set;
import java.util.function.Function;

public interface Trie<K, V> extends Store<K, V> {
    static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    // revert to another trie with rootHash and store provided
    // throw exception if the rootHash not found in the store
    Trie<K, V> revert(byte[] rootHash, Store<byte[], byte[]> store) throws RuntimeException;

    // revert to another trie with rootHash and store currently used
    // throw exception if the rootHash not found in the store
    Trie<K, V> revert(byte[] rootHash) throws RuntimeException;

    // revert to empty
    Trie<K, V> revert();

    // build a new trie and get the root hash of this trie
    // you could rollback to this state later by revert to the root hash generated
    byte[] commit();

    // dump key of nodes
    Set<byte[]> dump();

    // get root hash of a non-dirty tree
    // if trie is null, return null hash
    // throw RuntimeException if this trie is dirty
    byte[] getRootHash() throws RuntimeException;

    // return a null hash, revert to null hash returns an empty trie;
    byte[] getNullHash();

    // return true is root node is not null and root node is dirty
    boolean isDirty();

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
