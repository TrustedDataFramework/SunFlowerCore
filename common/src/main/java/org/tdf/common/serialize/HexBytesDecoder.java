package org.tdf.common.serialize;

import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPDecoder;
import org.tdf.rlp.RLPElement;

public class HexBytesDecoder implements RLPDecoder<HexBytes> {
    @Override
    public HexBytes decode(RLPElement element) {
        if(element.isNull()) return null;
        return new HexBytes(element.asBytes());
    }
}
