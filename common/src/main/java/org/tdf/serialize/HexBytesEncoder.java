package org.tdf.serialize;


import org.tdf.common.HexBytes;
import org.tdf.rlp.RLPElement;
import org.tdf.rlp.RLPEncoder;
import org.tdf.rlp.RLPItem;

public class HexBytesEncoder implements RLPEncoder<HexBytes> {
    @Override
    public RLPElement encode(HexBytes o) {
        if(o == null) return RLPItem.NULL;
        return RLPItem.fromBytes(o.getBytes());
    }
}
