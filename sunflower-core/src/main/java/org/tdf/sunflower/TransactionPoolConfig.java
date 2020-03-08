package org.tdf.sunflower;

import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "sunflower.transaction-pool")
@Component
@Setter
public class TransactionPoolConfig {
    private long expiredIn;

    private long lockTimeout;

    public long getExpiredIn() {
        return expiredIn <= 0 ? 300 : expiredIn;
    }


    public long getLockTimeout() {
        return lockTimeout <= 0 ? 1 : lockTimeout;
    }
}
