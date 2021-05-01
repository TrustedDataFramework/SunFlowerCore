package org.tdf.common.util;

import org.tdf.rlp.RLPCodec;

public class RLPUtil {
    public static <T> T decode(HexBytes data, Class<T> clazz) {
        return RLPCodec.decode(data.getBytes(), clazz);
    }

    public static HexBytes encode(Object o) {
        return HexBytes.fromBytes(RLPCodec.encode(o));
    }
}
