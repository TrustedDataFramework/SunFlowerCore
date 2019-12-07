package org.tdf.trie;

import org.tdf.common.Store;
import org.tdf.store.MapStore;

// enhanced radix tree
public class TrieImpl {
    private Store<byte[], byte[]> cache;

    private Node root;

    public TrieImpl() {
        this.cache = new MapStore<>();
    }

    public TrieImpl(Store<byte[], byte[]> store) {
        this.cache = store;
    }
}
