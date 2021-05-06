package org.tdf.common.serialize;

import java.util.function.Function;

/**
 * encoder and decoder from a type to another type
 *
 * @param <K> type
 */
public interface Codec<K> {
    static Codec<byte[]> identity() {
        return (Codec<byte[]>) Codecs.IDENTITY;
    }

    static <K> Codec<K> newInstance(
        Function<? super K, byte[]> encoder,
        Function<byte[], ? extends K> decoder
    ) {
        return new Codec<K>() {
            @Override
            public Function<? super K, byte[]> getEncoder() {
                return encoder;
            }

            @Override
            public Function<byte[], ? extends K> getDecoder() {
                return decoder;
            }
        };
    }

    Function<? super K, byte[]> getEncoder();

    Function<byte[], ? extends K> getDecoder();
}
