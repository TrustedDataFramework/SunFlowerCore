package org.tdf.sunflower.vm.hosts

import org.tdf.evm.EvmHook
import org.tdf.lotusvm.common.OpCode
import org.tdf.lotusvm.runtime.Hook
import org.tdf.lotusvm.runtime.HostFunction
import org.tdf.lotusvm.runtime.Memory.Companion.PAGE_SIZE
import org.tdf.lotusvm.runtime.ModuleInstanceImpl

class Limit(val gasLimit: Long) : Hook, EvmHook {


    var runtimeGas: Long = 0
        private set

    var initialGas: Long = 0

    val totalGas: Long get() = initialGas + runtimeGas

    override fun onInstruction(ins: OpCode, module: ModuleInstanceImpl) {
        runtimeGas += 100
        if (totalGas > gasLimit) throw RuntimeException("gas overflow")
    }

    override fun onHostFunction(function: HostFunction, module: ModuleInstanceImpl) {
        runtimeGas += 100
        if (totalGas > gasLimit) throw RuntimeException("gas overflow")
    }

    override fun onNewFrame() {}
    override fun onFrameExit() {}
    override fun onMemoryGrow(beforeGrow: Int, afterGrow: Int) {
        if (afterGrow > MAX_MEMORY) println("memory size overflow")
    }

    // 1 evm op = 200 wasm op
    override fun onOp(op: Int) {
        runtimeGas += 2000
        if (totalGas > gasLimit) throw RuntimeException("gas overflow total gas = $totalGas, gasLimit = $gasLimit ")
    }


    companion object {
        const val MAX_MEMORY: Int = 256 * PAGE_SIZE // memory is limited to less than 256 page = 16mb
    }
}