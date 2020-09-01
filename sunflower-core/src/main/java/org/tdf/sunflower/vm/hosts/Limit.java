package org.tdf.sunflower.vm.hosts;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.tdf.lotusvm.runtime.Frame;
import org.tdf.lotusvm.runtime.Hook;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.runtime.ModuleInstanceImpl;
import org.tdf.lotusvm.types.Instruction;
import org.tdf.sunflower.ApplicationConstants;

public class Limit implements Hook {
    private long steps;

    private int frameDepth;

    public Limit fork(){
        this.frameDepth = 0;
        return this;
    }

    @Override
    public void onInstruction(Instruction ins, ModuleInstanceImpl module) {
        steps++;
        if (ApplicationConstants.VM_STEP_LIMIT != 0 && steps > ApplicationConstants.VM_STEP_LIMIT)
            throw new RuntimeException("steps overflow");
    }

    @Override
    public void onHostFunction(HostFunction function, ModuleInstanceImpl module) {
        steps++;
        if (ApplicationConstants.VM_STEP_LIMIT != 0 && steps > ApplicationConstants.VM_STEP_LIMIT)
            throw new RuntimeException("steps overflow");
    }

    public long getGas() {
        return steps / 1024;
    }

    @Override
    public void onNewFrame(Frame frame) {
        this.frameDepth++;
        if(ApplicationConstants.MAX_FRAMES != 0 && this.frameDepth > ApplicationConstants.MAX_FRAMES)
            throw new RuntimeException("frames overflow");
    }

    @Override
    public void onFrameExit(Frame frame) {
        this.frameDepth--;
    }
}
