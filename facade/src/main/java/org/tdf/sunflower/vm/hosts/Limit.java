package org.tdf.sunflower.vm.hosts;

import lombok.Getter;
import org.tdf.lotusvm.runtime.*;
import org.tdf.lotusvm.types.Instruction;

public class Limit implements Hook {
    public static final int MAX_MEMORY = 256 * Memory.PAGE_SIZE; // memory is limited to less than 256 page = 16mb
    public static final long GAS_MULTIPLIER = 100; // gas = steps / gas multipiler


    public Limit(long gasLimit) {
        this.y = gasLimit * GAS_MULTIPLIER + GAS_MULTIPLIER - 1;
    }

    public Limit() {
    }

    @Getter
    private long steps;

    private long y;


    @Getter
    private long initialGas;

    public void setInitialGas(long initialGas) {
        this.initialGas = initialGas;
        this.y = this.y - GAS_MULTIPLIER * initialGas;
        if (this.y < 0)
            throw new RuntimeException("gas overflow");
    }

    @Override
    public void onInstruction(Instruction ins, ModuleInstanceImpl module) {
        steps++;
        if(steps > y)
            throw new RuntimeException("gas overflow");
    }

    @Override
    public void onHostFunction(HostFunction function, ModuleInstanceImpl module) {
        steps++;
        if(steps > y)
            throw new RuntimeException("gas overflow");
    }

    public long getGas() {
        return initialGas + steps / GAS_MULTIPLIER;
    }

    @Override
    public void onNewFrame() {
    }

    @Override
    public void onFrameExit() {
    }

    @Override
    public void onMemoryGrow(int beforeGrow, int afterGrow) {
        if(afterGrow > MAX_MEMORY)
            System.out.println("memory size overflow");
    }
}
