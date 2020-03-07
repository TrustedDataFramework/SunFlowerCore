package org.tdf.sunflower;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "sunflower.message-queue")
@Component
@Data
public class MessageQueueConfig {
    private String name;
    private int port;
}
