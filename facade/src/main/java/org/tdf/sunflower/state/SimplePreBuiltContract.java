package org.tdf.sunflower.state;


import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.vm.abi.Abi;

import static org.tdf.sunflower.state.Constants.SIMPLE_BIOS_CONTRACT_ADDR;

public class SimplePreBuiltContract implements BuiltinContract {
    @Override
    public HexBytes getAddress() {
        return SIMPLE_BIOS_CONTRACT_ADDR;
    }

    @Override
    public Abi getAbi() {
        return Abi.fromJson("[]");
    }
}
