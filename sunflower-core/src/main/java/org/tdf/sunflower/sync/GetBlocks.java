package org.tdf.sunflower.sync;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetBlocks {
    private long startHeight;
    private long stopHeight;
    private boolean descend;
}
