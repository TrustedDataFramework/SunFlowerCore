package org.tdf.sunflower.vm.abi;

import java.util.ArrayList;
import java.util.List;

public class MemoryUtil {
    private static final int MINIMUM_UNIT = 1 << 10;

    public static List<byte[]> splitAndCompress(byte[] data) {
        int offset = 0;
        List<byte[]> ret = new ArrayList<>();
        if (data.length % MINIMUM_UNIT != 0)
            throw new RuntimeException("invalid memory");
        while (offset < data.length) {
            byte[] tmp = new byte[MINIMUM_UNIT];
            System.arraycopy(data, offset, tmp, 0, tmp.length);
            if (isAllZero(tmp)) {
                ret.add(new byte[1]);
            } else {
                ret.add(tmp);
            }
            offset += MINIMUM_UNIT;
        }
        return ret;
    }

    private static boolean isAllZero(byte[] data) {
        for (byte datum : data) {
            if (datum != 0)
                return false;
        }
        return true;
    }
}
