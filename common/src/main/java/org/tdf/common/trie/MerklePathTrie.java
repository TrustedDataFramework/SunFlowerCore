package org.tdf.common.trie;

import org.tdf.rlp.RLPElement;

import java.util.Collection;

class MerklePathTrie<K, V> extends ReadOnlyTrie<K, V>{
    MerklePathTrie(AbstractTrie<K, V> delegate) {
        super(delegate);
    }

    @Override
    public RLPElement getProof(Collection<? extends K> keys) {
        throw new UnsupportedOperationException();
    }
}
