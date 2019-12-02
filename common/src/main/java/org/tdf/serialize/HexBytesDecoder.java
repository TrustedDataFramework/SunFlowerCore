package org.tdf.serialize;

import org.tdf.common.HexBytes;

public class HexBytesDecoder implements RLPDecoder<HexBytes> {
    @Override
    public HexBytes decode(RLPElement element) {
        if(element.isNull()) return null;
        return new HexBytes(element.getAsItem().get());
    }
}
