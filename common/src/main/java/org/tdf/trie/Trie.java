package org.tdf.trie;

import org.tdf.store.Store;

public interface Trie<K, V> extends Store<K, V> {
    // create a snap shot
    Trie<K, V> createSnapshot();
    // get root hash of current trie
    byte[] getRootHash();
}
