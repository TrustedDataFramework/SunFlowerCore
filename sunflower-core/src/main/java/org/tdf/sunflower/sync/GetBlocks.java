package org.tdf.sunflower.sync;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class GetBlocks {
    private long startHeight;
    private long stopHeight;
    private boolean descend;
    private int limit;

    GetBlocks clip() {
        if (stopHeight - startHeight < limit) return this;
        if (descend) {
            startHeight = stopHeight - limit;
        } else {
            stopHeight = startHeight + limit;
        }
        return this;
    }
}
