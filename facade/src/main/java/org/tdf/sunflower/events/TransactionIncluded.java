package org.tdf.sunflower.events;

import lombok.Value;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Event;
import org.tdf.sunflower.types.Transaction;

import java.util.List;

@Value
public class TransactionIncluded {
    Transaction transaction;
    Block block;
    long gasUsed;
    RLPList returns;
    List<Event> events;
}
