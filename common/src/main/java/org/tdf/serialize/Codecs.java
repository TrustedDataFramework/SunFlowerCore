package org.tdf.serialize;

import java.nio.charset.StandardCharsets;

public class Codecs {
    /**
     * Converter from string to byte array and vice versa
     */
    public static final Codec<String, byte[]> STRING = Codec
            .newInstance(
                    (x) -> x.getBytes(StandardCharsets.UTF_8),
                    x -> new String(x, StandardCharsets.UTF_8)
            );

}
