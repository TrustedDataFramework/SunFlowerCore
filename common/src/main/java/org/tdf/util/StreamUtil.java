package org.tdf.util;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StreamUtil {
    public static <K, V, R, U> Map<R, U> mapToMap(Map<K, V> map, Function<K, R> keyMap, Function<V, U> valueMap){
        return map.keySet().stream().collect(Collectors.toMap(keyMap, y -> valueMap.apply(map.get(y))));
    }
}
