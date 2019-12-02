package org.tdf.util;

import java.math.BigInteger;
import java.nio.ByteBuffer;

public class IEEE754 {

    public static byte[] encodeIEEE754Float(float number) {
        int val=Float.floatToIntBits(number);
        return intToBytes(val);
    }

    public static byte[] encodeIEEE754Double(double number){
        long val=Double.doubleToLongBits(number);
        return longToBytes(val);
    }

    public static float decodeIEEE754Float(byte[] data){
        int val=byteArrayToInt(data);
        return Float.intBitsToFloat(val);
    }

    public static double decodeIEEE754Double(byte[] data){
        long val=byteArrayToLong(data);
        return Double.longBitsToDouble(val);
    }

    public static byte[] longToBytes(long val) {
        return ByteBuffer.allocate(Long.BYTES).putLong(val).array();
    }

    public static byte[] intToBytes(int val){
        return ByteBuffer.allocate(Integer.BYTES).putInt(val).array();

    }

    public static int byteArrayToInt(byte[] b) {
        if (b == null || b.length == 0)
            return 0;
        return new BigInteger(1, b).intValue();
    }

    public static long byteArrayToLong(byte[] b) {
        if (b == null || b.length == 0)
            return 0;
        return new BigInteger(1, b).longValue();
    }
}
