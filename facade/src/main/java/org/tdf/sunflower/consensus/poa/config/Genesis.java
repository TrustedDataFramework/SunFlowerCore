package org.tdf.sunflower.consensus.poa.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.consensus.poa.PoAConstants;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.CryptoContext;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Genesis {
    public HexBytes parentHash;

    public long timestamp;
    public List<MinerInfo> miners;
    public Map<String, Long> alloc;
    public List<ValidatorInfo> validator;

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

    @Getter
    public static class MinerInfo {
        @JsonProperty("addr")
        public HexBytes address;
    }

    @Getter
    public static class ValidatorInfo {
        @JsonProperty("addr")
        public HexBytes address;
    }

    // 去重后的矿工地址
    public List<HexBytes> filterMiners() {
        if (miners == null || miners.isEmpty())
            return Collections.emptyList();
        List<HexBytes> ret = new ArrayList<>();
        for (MinerInfo m : miners) {
            if (ret.contains(m.getAddress()))
                throw new RuntimeException("duplicated miner address");
            ret.add(m.getAddress());
        }
        return ret;
    }

    // 去重后的验证者公钥
    public List<HexBytes> filtersValidators() {
        if (validator == null || validator.isEmpty())
            return Collections.emptyList();
        List<HexBytes> ret = new ArrayList<>();
        for (ValidatorInfo m : validator) {
            if (ret.contains(m.getAddress()))
                throw new RuntimeException("duplicated validator pk");
            ret.add(m.getAddress());
        }
        return ret;
    }
}
