package org.tdf.sunflower.types;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tdf.common.types.Uint256;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Stat {
    // cpu usage
    private double cpu;

    // memory used (bytes)
    private long memoryUsed;

    // total memory (bytes)
    private long totalMemory;

    // the best block height
    private long height;

    // average block internal (last 10 block)
    private double averageBlockInterval;

    // average transaction fee (last
    private Uint256 averageGasPrice;

    private long transactionPoolSize;

    // if current consensus is pow, returns current difficulty
    private String currentDifficulty;

    // if is mining, the coin base exists in last 10 blocks
    private boolean mining;

    // blocks per day =
    private long blocksPerDay;

    // tds version
    @Builder.Default
    private String version = "v1.0.0";

    private String consensus;
}
