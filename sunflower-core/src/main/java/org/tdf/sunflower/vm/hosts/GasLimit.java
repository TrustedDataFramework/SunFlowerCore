package org.tdf.sunflower.vm.hosts;

import lombok.Getter;
import org.tdf.lotusvm.runtime.Hook;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.runtime.ModuleInstanceImpl;
import org.tdf.lotusvm.types.Instruction;
import org.tdf.sunflower.ApplicationConstants;

public class GasLimit implements Hook {
    @Getter
    private long gas;

    @Override
    public void onInstruction(Instruction ins, ModuleInstanceImpl module) {
        gas++;
        if(gas > ApplicationConstants.GAS_LIMIT) throw new RuntimeException("gas overflow");
    }

    @Override
    public void onHostFunction(HostFunction function, ModuleInstanceImpl module) {
        gas++;
        if(gas > ApplicationConstants.GAS_LIMIT) throw new RuntimeException("gas overflow");
    }
}
