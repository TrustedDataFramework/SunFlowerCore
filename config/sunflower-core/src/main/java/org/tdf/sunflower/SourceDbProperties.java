package org.tdf.sunflower;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Properties;

@ConfigurationProperties(prefix = "sunflower.database")
@Component
public class SourceDbProperties extends Properties {
}
