package org.tdf.common.types;

import org.tdf.common.util.HexBytes;

public interface Chained extends Hashed {
    HexBytes getHashPrev();
}
