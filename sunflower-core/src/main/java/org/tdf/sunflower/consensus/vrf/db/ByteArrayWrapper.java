package org.tdf.sunflower.consensus.vrf.db;

import org.tdf.sunflower.consensus.vrf.util.FastByteComparisons;

import java.io.Serializable;
import java.util.Arrays;

import static org.tdf.sunflower.util.ByteUtil.toHexString;


/**
 * @author Roman Mandeleil
 * @since 11.06.2014
 */
public class ByteArrayWrapper implements Comparable<ByteArrayWrapper>, Serializable {

    private static final long serialVersionUID = 7120319357455987329L;
    private final byte[] data;
    private int hashCode = 0;

    public ByteArrayWrapper(byte[] data) {
        if (data == null)
            throw new NullPointerException("Data must not be null");
        this.data = data;
        this.hashCode = Arrays.hashCode(data);
    }

    public boolean equals(Object other) {
        if (!(other instanceof ByteArrayWrapper))
            return false;
        byte[] otherData = ((ByteArrayWrapper) other).getData();
        return FastByteComparisons.compareTo(
                data, 0, data.length,
                otherData, 0, otherData.length) == 0;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public int compareTo(ByteArrayWrapper o) {
        return FastByteComparisons.compareTo(
                data, 0, data.length,
                o.getData(), 0, o.getData().length);
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return toHexString(data);
    }
}
