package org.tdf.common.util;


import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

@NoArgsConstructor
@AllArgsConstructor
@With
@lombok.Builder(builderClassName = "Builder")
public class LRUMap<K, V> extends LinkedHashMap<K, V> {
    @lombok.Builder.Default
    private int maximumSize = Integer.MAX_VALUE;

    @lombok.Builder.Default
    private BiConsumer<? super K, ? super V> hook = (k, v) -> {
    };

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        boolean ret = size() > maximumSize;
        if (ret) hook.accept(eldest.getKey(), eldest.getValue());
        return ret;
    }
}