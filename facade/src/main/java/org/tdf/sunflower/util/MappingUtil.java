package org.tdf.sunflower.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;

import java.util.Map;
import java.util.Properties;

public class MappingUtil {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(JsonParser.Feature.ALLOW_COMMENTS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static Map<String, Object> pojoToMap(Object pojo) {
        return OBJECT_MAPPER.convertValue(pojo, new TypeReference<Map<String, Object>>() {
        });
    }

    public static <T> T propertiesToPojo(Properties properties, Class<T> cls) {
        JavaPropsMapper mapper = new JavaPropsMapper();
        try {
            return mapper.readPropertiesAs(properties, cls);
        } catch (Exception e) {
            String schema = "";
            e.printStackTrace();
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

