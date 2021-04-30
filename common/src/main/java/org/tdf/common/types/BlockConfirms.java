package org.tdf.common.types;


import lombok.Value;
import org.tdf.common.util.HexBytes;

@Value
public class BlockConfirms {
    long confirms;
    HexBytes blockHash;
    Long blockHeight;
}
