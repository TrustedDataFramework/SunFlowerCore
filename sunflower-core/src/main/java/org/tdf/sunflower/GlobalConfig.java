package org.tdf.sunflower;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@ConfigurationProperties(prefix = "sunflower")
@Component
public class GlobalConfig extends HashMap {
}
