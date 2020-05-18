package org.tdf.sunflower.consensus.pow;

import org.tdf.common.serialize.Codec;
import org.tdf.common.store.ByteArrayMapStore;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.crypto.CryptoContext;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.util.Collections;
import java.util.Map;

public class Genesis {
    public HexBytes parentHash;

    public long timestamp;

    private Map<HexBytes, Long> alloc;

    public Block get() {
        Trie<?, ?> trie = Trie.<byte[], byte[]>builder()
                .keyCodec(Codec.identity())
                .valueCodec(Codec.identity())
                .store(new ByteArrayMapStore<>())
                .hashFunction(CryptoContext::digest)
                .build();
        HexBytes emptyRoot = Transaction.getTransactionsRoot(Collections.emptyList());

        Header h = Header.builder()
                .version(PoW.BLOCK_VERSION)
                .hashPrev(parentHash)
                .stateRoot(HexBytes.fromBytes(trie.getNullHash()))
                .transactionsRoot(emptyRoot)
                .height(0)
                .payload(HexBytes.EMPTY)
                .createdAt(timestamp)
                .build();
        return new Block(h);
    }
}
