package org.tdf.sunflower;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Properties;

@ConfigurationProperties(prefix = "sunflower.consensus")
@Component
public class ConsensusProperties extends Properties{
    private static final long serialVersionUID = 3081921213768968804L;
    static final String CONSENSUS_NAME = "name";
}

