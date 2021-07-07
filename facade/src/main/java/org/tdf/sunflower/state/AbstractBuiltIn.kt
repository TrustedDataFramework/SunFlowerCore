package org.tdf.sunflower.state

import org.tdf.sunflower.vm.CallContext.Companion.empty
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.facade.RepositoryService
import org.tdf.sunflower.facade.RepositoryReader
import org.tdf.sunflower.vm.CallData
import org.tdf.sunflower.vm.abi.Abi
import org.tdf.common.util.FastByteComparisons
import org.tdf.common.util.hex
import org.tdf.sunflower.vm.Backend
import org.tdf.sunflower.vm.CallContext

abstract class AbstractBuiltIn protected constructor(
    override var address: HexBytes,
    protected var accounts: StateTrie<HexBytes, Account>,
    protected var repo: RepositoryService
) : BuiltinContract {

    protected fun getSelector(data: HexBytes): ByteArray {
        return data.slice(0, 4).bytes
    }

    override fun call(rd: RepositoryReader, backend: Backend, ctx: CallContext, callData: CallData): ByteArray {
        val func = getFunction(callData.data)
        val inputs = func.decode(callData.data.bytes)
        val results = call(rd, backend, ctx, callData, func.name, inputs.toTypedArray())
        return Abi.Entry.Param.encodeList(func.outputs, *results.toTypedArray())
    }

    protected fun getFunction(data: HexBytes): Abi.Function {
        val selector = getSelector(data)
        return abi.findFunction { FastByteComparisons.equal(it.encodeSignature(), selector) }!!
    }

    protected fun getFunction(method: String): Abi.Function {
        return abi.findFunction { it.name == method }!!
    }

    override fun view(rd: RepositoryReader, blockHash: HexBytes, method: String, vararg args: Any): List<*> {
        val parent = rd.getHeaderByHash(blockHash)!!
        val func = getFunction(method)
        val encoded = func.encode(*args)
        val callData = CallData(data = encoded.hex())
        return call(
            rd,
            accounts.createBackend(parent, isStatic = true),
            empty(),
            callData,
            method,
            *args
        )
    }
}