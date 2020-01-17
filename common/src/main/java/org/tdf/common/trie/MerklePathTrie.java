package org.tdf.common.trie;

import org.tdf.rlp.RLPElement;

public class MerklePathTrie<K, V> extends ReadOnlyTrie<K, V>{
    MerklePathTrie(Trie<K, V> delegate) {
        super(delegate);
    }

    @Override
    public RLPElement getMerklePath(K k) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] commit() {
        return delegate.commit();
    }
}
