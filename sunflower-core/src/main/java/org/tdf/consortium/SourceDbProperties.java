package org.tdf.consortium;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Properties;

@ConfigurationProperties(prefix = "consortium.database")
@Component
public class SourceDbProperties extends Properties {
}
