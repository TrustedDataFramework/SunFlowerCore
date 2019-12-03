package org.tdf.sunflower.util;

import org.apache.commons.codec.binary.Hex;

/**
 * @author Roman Mandeleil
 * @since 21.04.14
 */
public class RLPItem implements RLPElement {

    private final byte[] rlpData;

    public RLPItem(byte[] rlpData) {
        this.rlpData = rlpData;
    }

    @Override
    public byte[] getRLPBytes() {
        if (rlpData.length == 0)
            return null;
        return rlpData;
    }

    @Override
    public String getRLPHexString() {
        if (rlpData.length == 0)
            return null;
        return Hex.encodeHexString(rlpData);
    }


    @Override
    public String getRLPString() {
        if (rlpData.length == 0)
            return null;
        return new String(rlpData);
    }

    @Override
    public int getRLPInt() {
        if (rlpData.length == 0)
            return -1;
        return ByteUtil.byteArrayToInt(rlpData);
    }

    @Override
    public byte getRLPByte() {
        if (rlpData.length == 0)
            return -1;
        return rlpData[0];
    }

    @Override
    public long getRLPLong() {
        if(rlpData.length == 0){
            return -1;
        }
        return ByteUtil.byteArrayToLong(rlpData);
    }
}
