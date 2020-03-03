package org.tdf.sunflower;

import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "sunflower.sync")
@Component
@Setter
public class SyncConfig {
    private long heartRate;

    private long blockWriteRate;

    private long maxPendingBlocks;

    private int maxBlocksTransfer;

    public long getHeartRate() {
        return heartRate > 0 ? heartRate : 5;
    }

    public long getBlockWriteRate() {
        return blockWriteRate > 0 ? blockWriteRate : 1;
    }

    public long getMaxPendingBlocks() {
        return maxPendingBlocks >= 0 ? maxPendingBlocks : 2048;
    }

    public int getMaxBlocksTransfer() {
        return maxBlocksTransfer > 0 ? maxBlocksTransfer : 2048;
    }
}
