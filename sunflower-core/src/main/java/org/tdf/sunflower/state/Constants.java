package org.tdf.sunflower.state;

import org.tdf.common.util.HexBytes;

public class Constants {
    public static final HexBytes SIMPLE_BIOS_CONTRACT_ADDR = HexBytes.fromHex("0000000000000000000000000000000000000000");
    public static final String VRF_BIOS_CONTRACT_ADDR = "0000000000000000000000000000000000000001";
    public static final HexBytes POW_BIOS_ADDR = HexBytes.fromHex("0000000000000000000000000000000000000002");
    public static final HexBytes AUTHENTICATION_ADDR = HexBytes.fromHex("0000000000000000000000000000000000000003");
    public static final HexBytes VRF_BIOS_CONTRACT_ADDR_HEX_BYTES = HexBytes.fromHex(VRF_BIOS_CONTRACT_ADDR);
}
