package org.tdf.sunflower.consensus.pow;

import lombok.Data;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;

import java.util.Map;

@Data
public class Genesis {
    private HexBytes parentHash;

    private long timestamp;

    private HexBytes nbits;

    private Map<String, Long> alloc;

    public Block get() {
        if (nbits.size() != 32)
            throw new RuntimeException("invalid nbits size should be 32");

        Header h = Header.builder()
                .hashPrev(parentHash)
                .createdAt(timestamp)
                .build();

        return new Block(h);
    }
}
