package org.tdf.sunflower.vm.hosts;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tdf.lotusvm.runtime.Frame;
import org.tdf.lotusvm.runtime.Hook;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.runtime.ModuleInstanceImpl;
import org.tdf.lotusvm.types.Instruction;

@AllArgsConstructor
@NoArgsConstructor
public class Limit implements Hook {
    private static long VM_STEP_LIMIT = 0;
    private static long MAX_FRAMES = 0;

    public static void setVMStepLimit(long limit) {
        VM_STEP_LIMIT = limit;
    }

    public static void setMaxFrames(long limit) {
        MAX_FRAMES = limit;
    }

    @Getter
    @Setter
    private long steps;

    private int frameDepth;

    private long gasLimit;


    public void addGas(long gas) {
        initialGas += gas;
    }

    @Getter
    @Setter
    private long initialGas;


    @Override
    public void onInstruction(Instruction ins, ModuleInstanceImpl module) {
        steps++;
        if (VM_STEP_LIMIT != 0 && steps > VM_STEP_LIMIT)
            throw new RuntimeException("steps overflow");
        if (gasLimit > 0 && getGas() > gasLimit)
            throw new RuntimeException("gas overflow");
    }

    @Override
    public void onHostFunction(HostFunction function, ModuleInstanceImpl module) {
        steps++;
        if (VM_STEP_LIMIT != 0 && steps > VM_STEP_LIMIT)
            throw new RuntimeException("steps overflow");
        if (gasLimit > 0 && getGas() > gasLimit)
            throw new RuntimeException("gas overflow");
    }

    public long getGas() {
        return initialGas + steps / 1024;
    }

    @Override
    public void onNewFrame(Frame frame) {
        this.frameDepth++;
        if (MAX_FRAMES != 0 && this.frameDepth > MAX_FRAMES)
            throw new RuntimeException("frames overflow");
    }

    @Override
    public void onFrameExit(Frame frame) {
        this.frameDepth--;
    }
}
