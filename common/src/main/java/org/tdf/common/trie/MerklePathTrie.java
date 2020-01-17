package org.tdf.common.trie;

import org.tdf.rlp.RLPElement;

class MerklePathTrie<K, V> extends ReadOnlyTrie<K, V>{
    MerklePathTrie(AbstractTrie<K, V> delegate) {
        super(delegate);
    }

    @Override
    public RLPElement getMerklePath(K k) {
        throw new UnsupportedOperationException();
    }
}
