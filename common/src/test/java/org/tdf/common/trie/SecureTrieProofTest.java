package org.tdf.common.trie;

import org.tdf.common.serialize.Codecs;
import org.tdf.common.store.ByteArrayMapStore;
import org.tdf.common.util.HashUtil;

public class SecureTrieProofTest extends ProofTest {
    @Override
    Trie<String, String> supplyTrie() {
        Trie<String, String> delegate = Trie.<String, String>builder()
                .store(new ByteArrayMapStore<>())
                .valueCodec(Codecs.STRING)
                .keyCodec(Codecs.STRING)
                .hashFunction(HashUtil::sha3)
                .build();
        return new SecureTrie<>(delegate, HashUtil::sha3);
    }
}
