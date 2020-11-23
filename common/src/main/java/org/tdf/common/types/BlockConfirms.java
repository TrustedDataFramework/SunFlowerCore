package org.tdf.common.types;


import lombok.Value;
import org.tdf.common.util.HexBytes;

@Value
public class BlockConfirms {
    private long confirms;
    private HexBytes blockHash;
    private Long blockHeight;
}
