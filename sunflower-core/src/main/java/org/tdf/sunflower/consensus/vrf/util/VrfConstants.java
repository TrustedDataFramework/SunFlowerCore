package org.tdf.sunflower.consensus.vrf.util;

import org.tdf.common.util.HexBytes;
import org.tdf.common.util.BigEndian;

public class VrfConstants {
    public static final HexBytes ZERO_BYTES = HexBytes.fromBytes(new byte[32]);
    public static final int BLOCK_VERSION = BigEndian.decodeInt32(new byte[] { 0, 'v', 'r', 'f' });
    public static final int TRANSACTION_VERSION = BigEndian.decodeInt32(new byte[] { 0, 'v', 'r', 'f' });

    public final static long MESSAGE_TTL = 1000;

}
