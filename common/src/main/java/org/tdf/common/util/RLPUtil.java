package org.tdf.common.util;

import org.tdf.rlpstream.Rlp;

public class RLPUtil {
    public static <T> T decode(HexBytes data, Class<T> clazz) {
        return Rlp.decode(data.getBytes(), clazz);
    }

    public static HexBytes encode(Object o) {
        return HexBytes.fromBytes(Rlp.encode(o));
    }
}
