package org.tdf.common.trie;

import org.tdf.common.HashUtil;
import org.tdf.common.serialize.Codecs;
import org.tdf.common.store.ByteArrayMapStore;

public class TrieImplProofTest extends ProofTest {
    @Override
    Trie<String, String> supplyTrie() {
        return Trie.<String, String>builder()
                .store(new ByteArrayMapStore<>())
                .valueCodec(Codecs.STRING)
                .keyCodec(Codecs.STRING)
                .hashFunction(HashUtil::sha3)
                .build();
    }
}
