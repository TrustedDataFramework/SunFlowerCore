package org.tdf.sunflower.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import org.tdf.sunflower.Start;

import java.util.Map;
import java.util.Properties;

public class MappingUtil {
    public static Map<String, Object> pojoToMap(Object pojo) {
        return Start.MAPPER.convertValue(pojo, new TypeReference<Map<String, Object>>() {
        });
    }

    public static <T> T propertiesToPojo(Properties properties, Class<T> cls) {
        JavaPropsMapper mapper = new JavaPropsMapper();
        try {
            return mapper.readPropertiesAs(properties, cls);
        } catch (Exception e) {
            String schema = "";
            try {
                schema = mapper.writeValueAsProperties(cls.newInstance()).toString();
            } catch (Exception ignored) {

            }
            throw new RuntimeException(
                    "load properties failed :" + properties.toString() + " expecting " + schema
            );
        }
    }
}

