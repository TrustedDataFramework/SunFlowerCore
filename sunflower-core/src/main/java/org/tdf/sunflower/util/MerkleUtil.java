package org.tdf.sunflower.util;

import org.tdf.common.serialize.Codec;
import org.tdf.common.store.ByteArrayMapStore;
import org.tdf.common.trie.Trie;
import org.tdf.common.types.Hashed;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPCodec;

import java.util.Collection;
import java.util.function.Function;

public class MerkleUtil {
    public static HexBytes getMerkleRoot(Collection<? extends Hashed> nodes, Function<byte[], byte[]> hashFunction) {
        int i = 0;

        Trie<Integer, byte[]> trie = Trie.<Integer, byte[]>builder()
            .hashFunction(hashFunction)
            .keyCodec(Codec.newInstance(RLPCodec::encodeInt, RLPCodec::decodeInt))
            .valueCodec(Codec.identity())
            .store(new ByteArrayMapStore<>())
            .build();

        for (Hashed n : nodes) {
            trie.set(i, n.getHash().getBytes());
            i++;
        }
        return trie.commit();
    }
}
