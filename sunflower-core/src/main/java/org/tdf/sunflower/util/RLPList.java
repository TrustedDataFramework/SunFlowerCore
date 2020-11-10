package org.tdf.sunflower.util;

import org.apache.commons.codec.binary.Hex;

import java.util.ArrayList;

/**
 * @author Roman Mandeleil
 * @since 21.04.14
 */
public class RLPList extends ArrayList<RLPElement> implements RLPElement {

    byte[] rlpData;

    public static void recursivePrint(RLPElement element) {

        if (element == null)
            throw new RuntimeException("RLPElement object can't be null");
        if (element instanceof RLPList) {

            RLPList rlpList = (RLPList) element;
            System.out.print("[");
            for (RLPElement singleElement : rlpList)
                recursivePrint(singleElement);
            System.out.print("]");
        } else {
            String hex = Hex.encodeHexString(element.getRLPBytes());
            System.out.print(hex + ", ");
        }
    }

    public void setRLPData(byte[] rlpData) {
        this.rlpData = rlpData;
    }

    @Override
    public byte[] getRLPBytes() {
        return rlpData;
    }

    @Override
    public String getRLPHexString() {
        return Hex.encodeHexString(rlpData);
    }

    @Override
    public String getRLPString() {
        return new String(rlpData);
    }

    @Override
    public int getRLPInt() {
        return ByteUtil.byteArrayToInt(rlpData);
    }

    @Override
    public byte getRLPByte() {
        return rlpData[0];
    }

    @Override
    public long getRLPLong() {
        return ByteUtil.byteArrayToLong(rlpData);
    }
}
