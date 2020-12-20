package org.tdf.sunflower.events;

import lombok.Value;
import org.tdf.common.util.HexBytes;

@Value
public class TransactionConfirmed {
    HexBytes txHash;
}
