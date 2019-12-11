package org.tdf.serialize;

import org.tdf.common.HexBytes;
import org.tdf.rlp.RLPDecoder;
import org.tdf.rlp.RLPElement;

public class HexBytesDecoder implements RLPDecoder<HexBytes> {
    @Override
    public HexBytes decode(RLPElement element) {
        if(element.isNull()) return null;
        return new HexBytes(element.getAsItem().get());
    }
}
