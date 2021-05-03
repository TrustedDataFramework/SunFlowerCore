package org.tdf.sunflower.state;


import org.tdf.common.types.Uint256;
import org.tdf.sunflower.vm.Backend;
import org.tdf.sunflower.vm.CallData;
import org.tdf.sunflower.vm.abi.Abi;

import static org.tdf.sunflower.state.Constants.SIMPLE_BIOS_CONTRACT_ADDR;

public class SimplePreBuiltContract implements PreBuiltContract {
    @Override
    public Account getGenesisAccount() {
        return Account.emptyAccount(SIMPLE_BIOS_CONTRACT_ADDR, Uint256.ZERO);
    }

    @Override
    public Abi getAbi() {
        return Abi.fromJson("[]");
    }
}
