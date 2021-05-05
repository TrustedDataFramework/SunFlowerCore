package org.tdf.sunflower.consensus.pos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.AbstractGenesis;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;

import java.util.List;
import java.util.stream.Collectors;

public class Genesis extends AbstractGenesis {
    public List<MinerInfo> miners;

    public Genesis(JsonNode parsed) {
        super(parsed);
    }


    @JsonIgnore
    public Block getBlock() {
        Header h = Header.builder()
            .hashPrev(getParentHash())
            .createdAt(getTimestamp())
            .build();
        return new Block(h);
    }

    @Value
    public static class MinerInfo {
        HexBytes address;
        long vote;
    }

    private static MinerInfo fromJson(JsonNode n) {
        return new MinerInfo(
            HexBytes.fromHex(n.get("address").asText()),
            n.get("vote").asLong()
        );
    }

    public List<MinerInfo> getMiners() {
        return getArray("miners").stream()
            .map(Genesis::fromJson)
            .collect(Collectors.toList());
    }
}
