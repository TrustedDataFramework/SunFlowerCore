package org.tdf.sunflower.events;

import lombok.Value;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Event;

import java.util.List;

@Value
public class TransactionIncluded {
    HexBytes txHash;
    Block block;
    long gasUsed;
    RLPList returns;
    List<Event> events;
}
