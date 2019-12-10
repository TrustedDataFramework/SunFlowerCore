package org.tdf.serialize;

import com.google.common.base.Functions;

import java.util.function.Function;

public interface Codec<K, V> {
    Function<? super K, ? extends V> getEncoder();

    Function<? super V, ? extends K> getDecoder();

    static <K> Codec<K, K> identity() {
        return newInstance(Functions.identity(), Functions.identity());
    }

    static <K, V> Codec<K, V> newInstance(Function<? super K, ? extends V> encoder, Function<? super V, ? extends K> decoder) {
        return new Codec<K, V>() {
            @Override
            public Function<? super K, ? extends V> getEncoder() {
                return encoder;
            }

            @Override
            public Function<? super V, ? extends K> getDecoder() {
                return decoder;
            }
        };
    }
}
