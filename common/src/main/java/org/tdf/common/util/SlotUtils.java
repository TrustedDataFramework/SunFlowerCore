package org.tdf.common.util;

import java.math.BigInteger;

/**
 * slot is a 256bit arithmetic unit
 */
public final class SlotUtils {
    private SlotUtils() {
    }

    public static final int SLOT_SIZE = 8;
    public static final int SLOT_MAX_INDEX = SLOT_SIZE - 1;
    public static final long LONG_MASK = 0xffffffffL;
    public static final int[] ONE = {0, 0, 0, 0, 0, 0, 0, 1};

    public static final int[] ONE_EXT = {
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 1
    };

    public static final int MAX_BYTE_ARRAY_SIZE = 32;
    public static final int INT_SIZE = 4;
    public static final int INT_BITS = 32;


    /**
     * dst = left + right, return 1 if overflow
     */
    public static long add(int[] left, int leftOffset, int[] right, int rightOffset, int[] dst, int dstOffset) {
        long carry = 0;

        for (int i = SLOT_MAX_INDEX; i >= 0; i--) {
            long added = (left[leftOffset + i] & LONG_MASK) + (right[rightOffset + i] & LONG_MASK) + carry;
            dst[dstOffset + i] = (int) added;
            carry = added >>> INT_BITS;
        }
        return carry;
    }


    /**
     * write dst as left - right
     */
    public static void subMut(int[] left, int leftOffset, int[] right, int rightOffset, int[] dst, int dstOffset) {
        for (int i = rightOffset; i < rightOffset + SLOT_SIZE; i++) {
            right[i] = ~right[i];
        }
        add(right, rightOffset, ONE, 0, right, 0);
        add(left, leftOffset, right, rightOffset, dst, dstOffset);
    }

    /**
     * encode slot as big endian
     */
    public static void encodeBE(int[] left, int leftOffset, byte[] out, int outOffset) {
        for (int i = 0; i < SLOT_SIZE; i++) {
            BigEndian.encodeInt32(left[i + leftOffset], out, outOffset + i * INT_SIZE);
        }
    }

    /**
     * decode byte array into slot, require data.length - dataOffset >= 32
     */
    public static void decodeBE(byte[] data, int dataOffset, int[] out, int outOffset) {
        for (int i = 0; i < SLOT_SIZE; i++) {
            out[outOffset + i] = BigEndian.decodeInt32(data, dataOffset + i * INT_SIZE);
        }
    }

    /**
     * decode byte array into slot, require data.length - dataOffset <= 32
     */
    public static void copyFrom(int[] slot, int slotOffset, byte[] bytes) {
        if (bytes.length > MAX_BYTE_ARRAY_SIZE)
            throw new RuntimeException("byte array size overflow > " + MAX_BYTE_ARRAY_SIZE);

        if (bytes.length == MAX_BYTE_ARRAY_SIZE) {
            decodeBE(bytes, 0, slot, slotOffset);
            return;
        }

        byte[] data = new byte[MAX_BYTE_ARRAY_SIZE];
        System.arraycopy(bytes, 0, data, MAX_BYTE_ARRAY_SIZE - bytes.length, bytes.length);
        decodeBE(data, 0, slot, slotOffset);
    }

    /**
     * convert unsigned big integer to byte array
     */
    public static byte[] asUnsignedByteArray(
            BigInteger value) {
        byte[] bytes = value.toByteArray();

        if (bytes[0] == 0) {
            byte[] tmp = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, tmp, 0, tmp.length);
            return tmp;
        }
        return bytes;
    }

    /**
     * copy int into slot
     */
    public static void copyFrom(int[] slot, int slotOffset, int n) {
        slot[slotOffset + SLOT_SIZE - 1] = n;
    }

    /**
     * copy long into slot
     */
    public static void copyFrom(int[] slot, int slotOffset, long n) {
        slot[slotOffset + SLOT_SIZE - 1] = (int) n;
        slot[slotOffset + SLOT_SIZE - 2] = (int) (n >>> 32);
    }

    /**
     * copy big integer into slot
     */
    public static void copyFrom(int[] slot, int slotOffset, BigInteger bi) {
        copyFrom(slot, slotOffset, asUnsignedByteArray(bi));
    }

    /**
     * convert slot to big integer
     */
    public static BigInteger toBigInt(int[] slot, int slotOffset) {
        return toBigInt(slot, slotOffset, SLOT_SIZE);
    }

    /**
     * convert slot to big integer
     */
    public static BigInteger toBigInt(int[] slot, int slotOffset, int size) {
        byte[] bytes = new byte[size * INT_SIZE];
        for (int i = 0; i < size; i++)
            BigEndian.encodeInt32(slot[slotOffset + i], bytes, i * INT_SIZE);
        return new BigInteger(1, bytes);
    }

    /**
     * check if slot is zero
     */
    public static boolean isZero(int[] slot, int slotOffset) {
        for (int i = slotOffset; i < slotOffset + SLOT_SIZE; i++) {
            if (slot[i] != 0)
                return false;
        }
        return true;
    }

    /**
     * compare left slot to right slot
     */
    public static int compareTo(int[] left, int leftOffset, int[] right, int rightOffset, int limit) {
        for (int i = 0; i < limit; i++) {
            int compared = Integer.compareUnsigned(left[leftOffset + i], right[rightOffset + 1]);
            if (compared != 0)
                return compared;
        }
        return 0;
    }

    /**
     * keep the least significant 256 bit, write into out
     */
    public static void trim256(int[] data, int dataOffset, int size, int[] out, int outOffset) {
        if (size <= SLOT_SIZE) {
            System.arraycopy(data, dataOffset, out, SLOT_SIZE - size + outOffset, size);
            return;
        }
        System.arraycopy(data, dataOffset + size - SLOT_SIZE, out, outOffset, SLOT_SIZE);
    }

    public static void reset(int[] data) {
        reset(data, 0, data.length);
    }

    public static void reset(int[] data, int offset, int limit) {
        for (int i = offset; i < offset + limit; i++)
            data[i] = 0;
    }


    public static int intLen(int[] slot, int slotOffset, int limit) {
        for (int i = slotOffset; i < slotOffset + limit; i++) {
            if (slot[i] != 0)
                return limit - i;
        }
        return 0;
    }

}
