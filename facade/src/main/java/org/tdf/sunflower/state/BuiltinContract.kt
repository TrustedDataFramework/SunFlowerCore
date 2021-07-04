package org.tdf.sunflower.state

import org.tdf.common.util.ByteUtil
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.facade.RepositoryReader
import org.tdf.sunflower.vm.Backend
import org.tdf.sunflower.vm.CallContext
import org.tdf.sunflower.vm.CallData
import org.tdf.sunflower.vm.abi.Abi

interface BuiltinContract {
    val address: HexBytes
    val genesisStorage: Map<HexBytes, HexBytes>
        get() = emptyMap()

    /**
     * call builtin contract by abi encoded parameters
     */
    fun call(rd: RepositoryReader, backend: Backend, ctx: CallContext, callData: CallData): ByteArray {
        return ByteUtil.EMPTY_BYTE_ARRAY
    }

    /**
     * call builtin contract by var args
     */
    fun call(
        rd: RepositoryReader,
        backend: Backend,
        ctx: CallContext,
        callData: CallData,
        method: String,
        vararg args: Any
    ): List<*> {
        return emptyList<Any>()
    }

    val abi: Abi

    /**
     * static call
     */
    fun view(rd: RepositoryReader, blockHash: HexBytes, method: String, vararg args: Any): List<*> {
        return emptyList<Any>()
    }
}