package org.tdf.sunflower;

import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.tdf.common.util.HexBytes;

import java.util.Map;

@ConfigurationProperties(prefix = "sunflower.sync")
@Component
@Setter
public class SyncConfig {
    private long heartRate;

    private long blockWriteRate;

    private long maxPendingBlocks;

    private int maxBlocksTransfer;

    private String pruneHash;

    private long fastSyncHeight;

    private long lockTimeout;

    private String fastSyncHash;

    private int maxAccountsTransfer;
    private Map<String, Double> rateLimits;

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

    public int getMaxAccountsTransfer() {
        return maxAccountsTransfer > 0 ? maxAccountsTransfer : 512;
    }

    public byte[] getPruneHash() {
        return pruneHash == null ? new byte[0] : HexBytes.decode(pruneHash);
    }

    public byte[] getFastSyncHash() {
        return fastSyncHash == null ? null : HexBytes.decode(fastSyncHash);
    }

    public long getFastSyncHeight() {
        return this.fastSyncHeight;
    }

    public long getLockTimeout() {
        return this.lockTimeout;
    }

    public Map<String, Double> getRateLimits() {
        return this.rateLimits;
    }
}
