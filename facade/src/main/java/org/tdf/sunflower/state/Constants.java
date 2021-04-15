package org.tdf.sunflower.state;

import org.tdf.common.util.HexBytes;

public class Constants {
    public static final HexBytes SIMPLE_BIOS_CONTRACT_ADDR = HexBytes.fromHex("0000000000000000000000000000000000000000");
    public static final String VRF_BIOS_CONTRACT_ADDR = "0000000000000000000000000000000000000001";
    public static final HexBytes POW_BIOS_ADDR = HexBytes.fromHex("0000000000000000000000000000000000000002");
    public static final HexBytes PEER_AUTHENTICATION_ADDR = HexBytes.fromHex("0000000000000000000000000000000000000003");
    public static final HexBytes POA_AUTHENTICATION_ADDR = HexBytes.fromHex("0000000000000000000000000000000000000004");
    public static final HexBytes POS_CONTRACT_ADDR = HexBytes.fromHex("0000000000000000000000000000000000000005");
    public static final HexBytes VRF_BIOS_CONTRACT_ADDR_HEX_BYTES = HexBytes.fromHex(VRF_BIOS_CONTRACT_ADDR);
    public static final HexBytes VALIDATOR_CONTRACT_ADDR = HexBytes.fromHex("0000000000000000000000000000000000000006");

}
