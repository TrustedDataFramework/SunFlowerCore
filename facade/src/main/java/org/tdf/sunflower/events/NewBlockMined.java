package org.tdf.sunflower.events;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.Block;

import java.util.List;

@Value
@AllArgsConstructor
public class NewBlockMined {
    Block block;
    List<HexBytes> failedTransactions;
    List<String> reasons;
}
