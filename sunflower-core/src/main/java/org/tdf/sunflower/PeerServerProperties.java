package org.tdf.sunflower;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Properties;

@ConfigurationProperties(prefix = "sunflower.p2p")
@Component
public class PeerServerProperties extends Properties {
}
