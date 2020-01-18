package org.tdf.common.trie;

import java.util.Map;

class MerklePathTrie<K, V> extends ReadOnlyTrie<K, V> {
    MerklePathTrie(AbstractTrie<K, V> delegate) {
        super(delegate);
    }

    @Override
    public Map<byte[], byte[]> getProof(K k) {
        throw new UnsupportedOperationException();
    }
}
