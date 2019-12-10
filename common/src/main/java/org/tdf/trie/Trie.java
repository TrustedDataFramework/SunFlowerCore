package org.tdf.trie;

import org.tdf.store.Store;

public interface Trie extends Store<byte[], byte[]> {
    // create a snap shot
    Trie createSnapshot();
    // get root hash of current trie
    byte[] getRootHash();
}
