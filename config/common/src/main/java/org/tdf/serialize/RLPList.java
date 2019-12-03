package org.tdf.serialize;

import lombok.NonNull;
import lombok.experimental.Delegate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.tdf.serialize.RLPConstants.*;

public final class RLPList implements RLPElement, List<RLPElement> {
    public static RLPList of(RLPElement... elements){
        return new RLPList(Arrays.asList(elements));
    }

    public static RLPList fromElements(Collection<RLPElement> elements){
        return new RLPList(elements.stream().collect(Collectors.toList()));
    }

    public static RLPList createEmpty(){
        return new RLPList();
    }

    public static RLPList createEmpty(int cap){
        return new RLPList(new ArrayList<>(cap));
    }

    @Delegate
    public List<RLPElement> elements = new ArrayList<>();

    private RLPList(){}

    RLPList(List<RLPElement> elements){
        this.elements = elements;
    }

    @Override
    public boolean isList() {
        return true;
    }

    @Override
    public RLPList getAsList() {
        return this;
    }

    @Override
    public RLPItem getAsItem() {
        throw new RuntimeException("not a rlp item");
    }

    @Override
    public byte[] getEncoded() {
        return encodeList(stream().map(RLPElement::getEncoded).collect(Collectors.toList()));
    }

    @Override
    public boolean isNull() {
        return false;
    }

    public static byte[] encodeList(@NonNull Collection<byte[]> elements) {
        int totalLength = 0;
        for (byte[] element1 : elements) {
            totalLength += element1.length;
        }

        byte[] data;
        int copyPos;
        if (totalLength < SIZE_THRESHOLD) {

            data = new byte[1 + totalLength];
            data[0] = (byte) (OFFSET_SHORT_LIST + totalLength);
            copyPos = 1;
        } else {
            // length of length = BX
            // prefix = [BX, [length]]
            int tmpLength = totalLength;
            byte byteNum = 0;
            while (tmpLength != 0) {
                ++byteNum;
                tmpLength = tmpLength >> 8;
            }
            tmpLength = totalLength;
            byte[] lenBytes = new byte[byteNum];
            for (int i = 0; i < byteNum; ++i) {
                lenBytes[byteNum - 1 - i] = (byte) ((tmpLength >> (8 * i)) & 0xFF);
            }
            // first byte = F7 + bytes.length
            data = new byte[1 + lenBytes.length + totalLength];
            data[0] = (byte) (OFFSET_LONG_LIST + byteNum);
            System.arraycopy(lenBytes, 0, data, 1, lenBytes.length);

            copyPos = lenBytes.length + 1;
        }
        for (byte[] element : elements) {
            System.arraycopy(element, 0, data, copyPos, element.length);
            copyPos += element.length;
        }
        return data;
    }
}
