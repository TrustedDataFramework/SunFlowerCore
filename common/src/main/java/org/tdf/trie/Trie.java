package org.tdf.trie;

import org.tdf.store.Store;

public interface Trie<K, V> extends Store<K, V> {
    // move to another trie with rootHash and store provided
    // throw exception if the rootHash not found in the store
    Trie<K, V> moveTo(byte[] rootHash, Store<byte[], byte[]> store) throws RuntimeException;

    // commit to cache and get root hash of current trie
    byte[] commit();
}
