package org.tdf.trie;

import org.tdf.common.Store;

public interface Trie extends Store<byte[], byte[]> {
    // generate a snap shot
    byte[] getRootHash();
    void setRoot(byte[] rootHash);
    void flush();
}
