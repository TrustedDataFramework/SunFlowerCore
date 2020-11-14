package org.tdf.sunflower.sync;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.Block;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Proposal {
    private Block block;
    private List<HexBytes> failedTransactions;
    private List<String> reasons;
}
