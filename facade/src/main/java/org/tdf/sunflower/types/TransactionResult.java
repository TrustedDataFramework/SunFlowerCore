package org.tdf.sunflower.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tdf.rlp.RLPList;

import java.util.Collections;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResult {
    long gasUsed;
    RLPList returns;
    List<Event> events;
    public static final TransactionResult EMPTY = new TransactionResult(0, RLPList.createEmpty(), Collections.emptyList());
}
