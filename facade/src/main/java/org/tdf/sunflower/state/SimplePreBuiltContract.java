package org.tdf.sunflower.state;


import org.tdf.sunflower.vm.Backend;
import org.tdf.sunflower.vm.CallData;

import static org.tdf.sunflower.state.Constants.SIMPLE_BIOS_CONTRACT_ADDR;

public class SimplePreBuiltContract implements PreBuiltContract {
    @Override
    public Account getGenesisAccount() {
        return Account.emptyContract(SIMPLE_BIOS_CONTRACT_ADDR);
    }

    @Override
    public void update(Backend backend, CallData callData) {

    }
}
