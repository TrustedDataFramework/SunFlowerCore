package org.tdf.sunflower;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "sunflower.transaction-pool")
@Component
@Data
public class TransactionPoolConfig {
    private long expiredIn;
}
