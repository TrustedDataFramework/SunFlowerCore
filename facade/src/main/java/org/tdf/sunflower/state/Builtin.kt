package org.tdf.sunflower.state

import org.tdf.common.util.ByteUtil
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.facade.RepositoryReader
import org.tdf.sunflower.vm.Backend
import org.tdf.sunflower.vm.CallContext
import org.tdf.sunflower.vm.CallData
import org.tdf.sunflower.vm.abi.Abi

interface Builtin {
    val codeSize: Int get() = 0
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
    ): List<*>

    val abi: Abi

    /**
     * static call
     */
    fun view(rd: RepositoryReader, backend: Backend, method: String, vararg args: Any): List<*> {
        return emptyList<Any>()
    }
}

class LoggingContract : AbstractBuiltin(Constants.LOGGING_CONTRACT_ADDR) {
    override val codeSize: Int = 1
    override fun call(
        rd: RepositoryReader,
        backend: Backend,
        ctx: CallContext,
        callData: CallData,
        method: String,
        vararg args: Any
    ): List<*> {
        println(args[0])
        return emptyList<Any>()
    }

    override val abi: Abi = ABI


    companion object {
        const val abiJson = """
[
    {
        "inputs": [
            {
                "name": "arg",
                "type": "string"
            }
        ],
        "name": "log",
        "outputs": [],
        "stateMutability": "pure",
        "type": "function"
    }
]
        """
        val ABI = Abi.fromJson(abiJson)
    }
}