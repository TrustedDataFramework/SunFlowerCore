package org.tdf.common.util;


import com.github.salpadding.rlpstream.Rlp;
import com.github.salpadding.rlpstream.RlpList;
import com.github.salpadding.rlpstream.StreamId;

public class RLPUtil {
    public static <T> T decode(HexBytes data, Class<T> clazz) {
        return Rlp.decode(data.getBytes(), clazz);
    }

    public static HexBytes encode(Object o) {
        return HexBytes.fromBytes(Rlp.encode(o));
    }

    public static RlpList decodePartial(byte[] bin, int offset) {
        long streamId = StreamId.decodeElement(bin, offset, bin.length, false);
        return new RlpList(bin, streamId, 0);
    }
}
