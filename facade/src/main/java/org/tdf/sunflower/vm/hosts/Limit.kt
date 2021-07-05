package org.tdf.sunflower.vm.hosts

import org.tdf.lotusvm.runtime.Hook
import org.tdf.evm.EvmHook
import org.tdf.lotusvm.common.OpCode
import org.tdf.lotusvm.runtime.ModuleInstanceImpl
import org.tdf.lotusvm.runtime.HostFunction
import org.tdf.lotusvm.runtime.Memory.Companion.PAGE_SIZE

class Limit : Hook, EvmHook {
    constructor(gasLimit: Long) {
        y = gasLimit * GAS_MULTIPLIER + GAS_MULTIPLIER - 1
    }

    constructor()

    var steps: Long = 0
        private set
    private var y: Long = 0

    var initialGas: Long = 0
        set(value) {
            field = value
            y -= GAS_MULTIPLIER * value
            if (y < 0) throw RuntimeException("gas overflow")
        }

    override fun onInstruction(ins: OpCode, module: ModuleInstanceImpl) {
        steps++
        if (steps > y) throw RuntimeException("gas overflow")
    }

    override fun onHostFunction(function: HostFunction, module: ModuleInstanceImpl) {
        steps++
        if (steps > y) throw RuntimeException("gas overflow")
    }

    val gas: Long
        get() = initialGas + steps / GAS_MULTIPLIER

    override fun onNewFrame() {}
    override fun onFrameExit() {}
    override fun onMemoryGrow(beforeGrow: Int, afterGrow: Int) {
        if (afterGrow > MAX_MEMORY) println("memory size overflow")
    }

    // 1 evm op = 200 wasm op
    override fun onOp(op: Int) {
        steps += 200
        if (steps > y) throw RuntimeException("gas overflow")
    }


    companion object {
        const val MAX_MEMORY: Int = 256 * PAGE_SIZE // memory is limited to less than 256 page = 16mb
        const val GAS_MULTIPLIER: Long = 100 // gas = steps / gas multipiler
    }
}