package org.tdf.trie;

import org.tdf.store.Store;

import java.util.Set;
import java.util.function.BiConsumer;

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

    void traverse(BiConsumer<TrieKey, Node> action);

    // dump key of nodes
    Set<byte[]> dump();

    // get root hash of non-dirty tree
    // if trie is null, return null hash
    // throw RuntimeException if this trie is dirty
    byte[] getRootHash() throws RuntimeException;

    // return true is root node is not null and root node is dirty
    boolean isDirty();
}
