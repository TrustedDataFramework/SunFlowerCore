package org.tdf.sunflower;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "sunflower.database")
@Component
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DatabaseConfig {
    private String name;
    private int maxOpenFiles;
    private String directory;
    private boolean reset;
}
