package org.tdf.sunflower.events;

import lombok.Value;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.types.TransactionResult;

import java.util.List;
import java.util.Map;

@Value
public class TransactionOutput {
    long blockHeight;
    HexBytes blockHash;
    Map<HexBytes, TransactionResult> results;
    Map<HexBytes, List<Map.Entry<String, RLPList>>> events;
    Map<HexBytes, String> reasons; // failed transactions
}
