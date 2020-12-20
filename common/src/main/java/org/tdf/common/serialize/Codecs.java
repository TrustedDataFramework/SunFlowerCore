package org.tdf.common.serialize;

import org.tdf.rlp.RLPCodec;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public class Codecs {
    /**
     * Converter from string to byte array and vice versa
     */
    public static final Codec<String, byte[]> STRING = Codec
            .newInstance(
                    (x) -> x.getBytes(StandardCharsets.UTF_8),
                    x -> new String(x, StandardCharsets.UTF_8)
            );
    static Codec IDENTITY = Codec.newInstance(Function.identity(), Function.identity());

    public static <K> Codec<K, byte[]> newRLPCodec(Class<K> clazz) {
        return Codec.newInstance(RLPCodec::encode, x -> RLPCodec.decode(x, clazz));
    }
}
