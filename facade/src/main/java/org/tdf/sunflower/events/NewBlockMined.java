package org.tdf.sunflower.events;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.TransactionInfo;

import java.util.List;

// proposal new block and failed transactions to peers
@Value
@AllArgsConstructor
public class NewBlockMined {
    Block block;
    List<TransactionInfo> infos;
}
