package org.tdf.trie;

import org.tdf.store.Store;

public interface Trie<V> extends Store<byte[], V> {
    // create a snap shot
    Trie<V> createSnapshot();
    // get root hash of current trie
    byte[] getRootHash();
}
