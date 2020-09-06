package org.tdf.sunflower.events;

import lombok.Value;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Transaction;

@Value
public class TransactionIncluded {
    Transaction transaction;
    Block block;
}
