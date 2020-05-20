package org.tdf.sunflower.consensus.poa.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.consensus.poa.PoAConstants;
import org.tdf.sunflower.crypto.CryptoContext;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

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
    public Block getBlock() {

        HexBytes emptyRoot = Transaction.getTransactionsRoot(Collections.emptyList());

        Header h = Header.builder()
                .version(PoAConstants.BLOCK_VERSION)
                .hashPrev(PoAConstants.ZERO_BYTES)
                .stateRoot(HexBytes.fromBytes(CryptoContext.getEmptyTrieRoot()))
                .transactionsRoot(emptyRoot)
                .height(0)
                .payload(HexBytes.EMPTY)
                .createdAt(timestamp)
                .build();
        return new Block(h);
    }
}
