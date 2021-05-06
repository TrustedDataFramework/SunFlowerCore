package org.tdf.sunflower.controller;

import org.spongycastle.util.encoders.Hex;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.ByteUtil;
import org.tdf.common.util.HexBytes;

import java.math.BigInteger;

public class TypeConverter {

    public static BigInteger hexToBigInteger(String input) {
        if (input.startsWith("0x")) {
            return new BigInteger(input.substring(2), 16);
        } else {
            return new BigInteger(input, 10);
        }
    }

    public static byte[] hexToByteArray(String x) {
        return ByteUtil.hexStringToBytes(x);
    }

    public static long hexToLong(String x) {
        return ByteUtil.byteArrayToLong(Hex.decode(fromHex(x)));
    }

    public static long hexToInt(String x) {
        return ByteUtil.byteArrayToInt(Hex.decode(fromHex(x)));
    }

    private static String fromHex(String x) {
        if (x.startsWith("0x")) {
            x = x.substring(2);
        }
        if (x.length() % 2 != 0) x = "0" + x;
        return x;
    }

    /**
     * Stringify byte[] x
     * null for null
     * null for empty []
     */
    public static String toJsonHex(byte[] x) {
        return x == null || x.length == 0 ? null : "0x" + Hex.toHexString(x);
    }

    public static String toJsonHex(HexBytes x) {
        return x == null || x.size() == 0 ? null : "0x" + x.toHex();
    }

    public static String toJsonHexNumber(byte[] x) {
        if (x == null) {
            return "0x0";
        }
        String hex = Hex.toHexString(x);
        return toJsonHex(hex.isEmpty() ? "0" : hex);
    }

    public static String toJsonHex(String x) {
        return "0x" + x;
    }

    public static String toJsonHex(int x) {
        return toJsonHex((long) x);
    }

    public static String toJsonHex(Long x) {
        return x == null ? null : "0x" + Long.toHexString(x);
    }

    public static String toJsonHex(BigInteger n) {
        return "0x" + n.toString(16);
    }

    public static long jsonHexToLong(String x) throws Exception {
        if (!x.startsWith("0x"))
            throw new Exception("Incorrect hex syntax");
        x = x.substring(2);
        return Long.parseLong(x, 16);
    }

    public static int jsonHexToInt(String x) throws Exception {
        if (!x.startsWith("0x"))
            throw new Exception("Incorrect hex syntax");
        x = x.substring(2);
        return Integer.parseInt(x, 16);
    }

    public static String jsonHexToHex(String x) {
        if (!x.startsWith("0x"))
            throw new RuntimeException("Incorrect hex syntax");
        x = x.substring(2);
        return x;
    }

    public static Uint256 jsonHexToU256(String x) {
        return Uint256.of(
            ByteUtil.hexStringToBytes(x)
        );
    }

    public static HexBytes jsonHexToHexBytes(String x) {
        return HexBytes.fromBytes(ByteUtil.hexStringToBytes(x));
    }
}