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

//    public static <K, V> Builder<K, V> builder(){
//        return new Builder<>();
//    }
//
//    static class Builder<K, V> {
//        private int maximumSize = Integer.MAX_VALUE;
//        private BiConsumer<? super K, ? super V> hook = (k, v) -> {
//        };
//
//        public Builder<K, V> maximumSize(int maximumSize){
//            this.maximumSize = maximumSize;
//            return this;
//        }
//
//        public Builder<K, V> hook(BiConsumer<? super K, ? super V> hook){
//            this.hook = hook;
//            return this;
//        }
//
//        public <K1 extends K, V1 extends V>  LRUMap<K1, V1> build(){
//            return new LRUMap<>(maximumSize, hook);
//        }
//    }
}
