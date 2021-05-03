package org.tdf.sunflower.events;

import lombok.Value;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.types.VMResult;

import java.util.List;
import java.util.Map;

@Value
public class TransactionOutput {
    long blockHeight;
    HexBytes blockHash;
    Map<HexBytes, VMResult> results;
    Map<HexBytes, String> reasons; // failed transactions
}
