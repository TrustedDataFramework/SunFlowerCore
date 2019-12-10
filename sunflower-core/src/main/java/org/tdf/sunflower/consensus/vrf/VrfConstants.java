package org.tdf.sunflower.consensus.vrf;

import org.tdf.common.HexBytes;
import org.tdf.util.BigEndian;

public class VrfConstants {
    public static final HexBytes ZERO_BYTES = new HexBytes(new byte[32]);
    public static final int BLOCK_VERSION = BigEndian.decodeInt32(new byte[] { 0, 'v', 'r', 'f' });
    public static final int TRANSACTION_VERSION = BigEndian.decodeInt32(new byte[] { 0, 'v', 'r', 'f' });

    public final static long MESSAGE_TTL = 1000;

}
