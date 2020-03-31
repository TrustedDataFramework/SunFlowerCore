package org.tdf.sunflower.util;

import com.fasterxml.jackson.core.type.TypeReference;
import org.tdf.sunflower.Start;

import java.util.Map;

public class MappingUtil {
    public static Map<String, Object> pojoToMap(Object pojo) {
        return Start.MAPPER.convertValue(pojo, new TypeReference<Map<String, Object>>() {
        });
    }
}

