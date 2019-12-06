package org.tdf.sunflower.console;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "sunflower.console")
@Component
@Getter
@Setter
public class ConsoleConfig {
    private String tokenFile;
    private int port;
    private boolean disabled;
}
