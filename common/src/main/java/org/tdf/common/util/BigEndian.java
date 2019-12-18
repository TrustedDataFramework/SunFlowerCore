package org.tdf.common.util;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * util for big-endian encoding & decoding
 */
public class BigEndian {
    private static final BigInteger shadow;

    static {
        byte[] shadowBits = new byte[32];
        shadowBits[0] = (byte) 0xff;
        shadow = new BigInteger(1, shadowBits);
    }


    public static int decodeInt32(byte[] data) {
        return ByteBuffer.wrap(data)
                .getInt();
    }

    // big-endian encoding
    public static byte[] encodeInt32(int val) {
        return ByteBuffer.allocate(Integer.BYTES)
                .putInt(val).array();
    }

    // big-endian encoding
    public static byte[] encodeInt64(long value) {
        return ByteBuffer.allocate(Long.BYTES)
                .putLong(value).array();
    }

    public static long decodeInt64(byte[] data) {
        return ByteBuffer.wrap(data).getLong();
    }

    public static int compareUint256(byte[] a, byte[] b) {
        return new BigInteger(1, a).compareTo(
                new BigInteger(1, b)
        );
    }

    public static short decodeInt16(byte[] in) {
        return ByteBuffer.wrap(in).getShort();
    }

    public static byte[] encodeInt16(short value) {
        return ByteBuffer.allocate(Short.BYTES)
                .putShort(value).array();
    }

    public static byte[] encodeUint256(BigInteger in) {
        if (in.signum() < 0) {
            return null;
        }
        if (in.signum() == 0) {
            return new byte[32];
        }
        byte[] res = new byte[32];
        for (int i = 0; i < res.length; i++) {
            BigInteger tmp = in.and(shadow.shiftRight(i * 8)).shiftRight((res.length - i - 1) * 8);
            res[i] = tmp.byteValue();
        }
        return res;
    }

    public static BigInteger decodeUint256(byte[] in) {
        return new BigInteger(1, Arrays.copyOfRange(in, 0, 32));
    }

}
