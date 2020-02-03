package org.tdf.sunflower.consensus.poa.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.tdf.common.serialize.Codec;
import org.tdf.common.store.ByteArrayMapStore;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.crypto.HashFunctions;
import org.tdf.sunflower.consensus.poa.PoAConstants;
import org.tdf.sunflower.consensus.poa.PoAUtils;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;

import java.util.List;

public class Genesis {
    public HexBytes parentHash;

    public long timestamp;

    @Getter
    public static class MinerInfo {
        @JsonProperty("addr")
        public HexBytes address;
    }

    public List<MinerInfo> miners;

    @JsonIgnore
    public Block getBlock(){
        Trie<?, ?> trie = Trie.<byte[], byte[]>builder()
                            .keyCodec(Codec.identity())
                            .valueCodec(Codec.identity())
                            .store(new ByteArrayMapStore<>())
                            .hashFunction(HashFunctions::keccak256)
                            .build();

        Header h = Header.builder()
                .version(PoAConstants.BLOCK_VERSION)
                .hashPrev(PoAConstants.ZERO_BYTES)
                .stateRoot(HexBytes.fromBytes(trie.getNullHash()))
                .height(0)
                .createdAt(timestamp)
                .payload(PoAConstants.ZERO_BYTES).build();
        Block b = new Block(h);
        b.setHash(HexBytes.fromBytes(PoAUtils.getHash(b)));
        return b;
    }
}
