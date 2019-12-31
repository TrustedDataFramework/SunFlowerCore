package org.tdf.common.trie;

import org.tdf.common.store.Store;

import java.util.Set;
import java.util.function.BiConsumer;

public interface Trie<K, V> extends Store<K, V> {
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

    // iterate over the trie
    void traverse(BiConsumer<TrieKey, Node> action);

    // dump key of nodes
    Set<byte[]> dump();

    // get root hash of a non-dirty tree
    // if trie is null, return null hash
    // throw RuntimeException if this trie is dirty
    byte[] getRootHash() throws RuntimeException;

    // return true is root node is not null and root node is dirty
    boolean isDirty();
}
