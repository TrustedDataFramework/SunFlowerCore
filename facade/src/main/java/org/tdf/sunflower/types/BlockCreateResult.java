package org.tdf.sunflower.types;

import lombok.Value;
import org.tdf.common.util.HexBytes;

import java.util.Collections;
import java.util.List;

@Value
public class BlockCreateResult {
    public static final BlockCreateResult EMPTY = new BlockCreateResult(null, Collections.emptyList(), Collections.emptyList());
    Block block;
    List<HexBytes> failedTransactions;
    List<String> reasons;

    public static BlockCreateResult empty() {
        return EMPTY;
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }
}
