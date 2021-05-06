package org.tdf.sunflower.facade;

import lombok.Value;

import java.util.Properties;

@Value
public class PropertiesWrapper implements PropertyLike {
    Properties properties;

    @Override
    public String getProperty(String key) {
        return properties.getProperty(key);
    }
}
