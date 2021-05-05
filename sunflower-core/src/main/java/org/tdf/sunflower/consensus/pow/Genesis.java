package org.tdf.sunflower.consensus.pow;

import com.fasterxml.jackson.databind.JsonNode;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.AbstractGenesis;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;

public class Genesis extends AbstractGenesis {
    public Genesis(JsonNode parsed) {
        super(parsed);
    }

    public HexBytes getNbits() {
        JsonNode n = parsed.get("nbits");
        return n == null ?
            HexBytes.fromHex("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff") :
            HexBytes.fromHex(n.asText());
    }

    public Block getBlock() {
        if (getNbits().size() != 32)
            throw new RuntimeException("invalid nbits size should be 32");

        Header h = Header.builder()
            .hashPrev(getParentHash())
            .createdAt(getTimestamp())
            .build();

        return new Block(h);
    }
}
