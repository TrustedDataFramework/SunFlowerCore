package org.tdf.sunflower.types;

import lombok.Value;

import java.util.Collections;
import java.util.List;

@Value
public class BlockCreateResult {
    public static final BlockCreateResult EMPTY = new BlockCreateResult(null, Collections.emptyList());
    Block block;
    List<TransactionInfo> infos;

    public static BlockCreateResult empty() {
        return EMPTY;
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }
}
