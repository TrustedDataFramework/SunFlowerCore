package org.tdf.trie;

import org.tdf.store.Store;

import java.util.Set;

/**
 * Trie's implementation will cache modifications in memory, before you call Trie.flush() method,
 * the underlying store will not be modified
 */
public interface Trie<K, V> extends Store<K, V> {
    // move to another trie with rootHash and store provided
    // throw exception if the rootHash not found in the store
    Trie<K, V> moveTo(byte[] rootHash, Store<byte[], byte[]> store) throws RuntimeException;

    // build a new trie and get the root hash of this trie
    // you could rollback to this Trie later by move to the root hash generated
    byte[] commit();

    void traverse(ScannerAction action);

    // dump key of nodes
    Set<byte[]> dump();
}
