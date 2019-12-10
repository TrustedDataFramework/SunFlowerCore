package org.tdf.trie;

import org.tdf.store.Store;

public interface Trie extends Store<byte[], byte[]> {
    // commit modifications and return a new trie
    Trie commit();
    byte[] getRootHash();
    boolean isDirty();
}
