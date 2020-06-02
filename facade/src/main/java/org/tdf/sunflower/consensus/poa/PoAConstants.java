package org.tdf.sunflower.consensus.poa;

import org.tdf.common.util.HexBytes;
import org.tdf.common.util.LittleEndian;

public class PoAConstants {
    public static final HexBytes ZERO_BYTES = HexBytes.fromBytes(new byte[32]);
    public static final int BLOCK_VERSION = LittleEndian.decodeInt32(new byte[]{0, 'p', 'o', 'a'});
    public static final int TRANSACTION_VERSION = LittleEndian.decodeInt32(new byte[]{0, 'p', 'o', 'a'});
}
