package org.tdf.sunflower.consensus.poa.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.tdf.common.serialize.Codec;
import org.tdf.common.store.ByteArrayMapStore;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.crypto.CryptoContext;
import org.tdf.sunflower.consensus.poa.PoAConstants;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.types.UnmodifiableBlock;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Genesis {
    public HexBytes parentHash;

    public long timestamp;

    @Getter
    public static class MinerInfo {
        @JsonProperty("addr")
        public HexBytes address;
    }

    public List<MinerInfo> miners;

    public Map<String, Long> alloc;

    @JsonIgnore
    public Block getBlock(){
        Trie<?, ?> trie = Trie.<byte[], byte[]>builder()
                            .keyCodec(Codec.identity())
                            .valueCodec(Codec.identity())
                            .store(new ByteArrayMapStore<>())
                            .hashFunction(CryptoContext::digest)
                            .build();
        byte[] emptyRoot = Transaction.getTransactionsRoot(Collections.emptyList());

        Header h = Header.builder()
                .version(PoAConstants.BLOCK_VERSION)
                .hashPrev(PoAConstants.ZERO_BYTES)
                .stateRoot(HexBytes.fromBytes(trie.getNullHash()))
                .transactionsRoot(HexBytes.fromBytes(emptyRoot))
                .height(0)
                .createdAt(timestamp)
                .payload(PoAConstants.ZERO_BYTES).build();
        return new Block(h);
    }
}
