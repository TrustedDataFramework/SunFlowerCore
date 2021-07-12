package org.tdf.sunflower.vm

import org.tdf.common.types.Uint256
import org.tdf.common.util.*
import org.tdf.evm.Digest
import org.tdf.evm.EvmHost
import org.tdf.sunflower.facade.RepositoryReader
import org.tdf.sunflower.types.LogInfo
import java.math.BigInteger

val sha3 = Digest { src: ByteArray, srcPos: Int, srcLen: Int,
                    dst: ByteArray, dstPos: Int ->
    HashUtil.sha3(src, srcPos, srcLen, dst, dstPos)
}

class EvmHostImpl(private val executor: VMExecutor, private val rd: RepositoryReader) : EvmHost {
    val backend: Backend = executor.backend

    override val digest: Digest
        get() = sha3

    override fun getBalance(address: ByteArray): BigInteger {
        return backend.getBalance(address.hex()).value
    }

    override fun getStorage(address: ByteArray, key: ByteArray): ByteArray {
        return backend.dbGet(
            address.hex(),
            key.hex()
        ).bytes
    }

    override fun setStorage(address: ByteArray, key: ByteArray, value: ByteArray) {
        backend.dbSet(
            address.hex(),
            key.hex(),
            value.hex()
        )
    }

    override fun getCode(addr: ByteArray): ByteArray {
        return backend.getCode(addr.hex()).bytes
    }


    override fun call(
        caller: ByteArray,
        receipt: ByteArray,
        input: ByteArray,
        // when static call = true, value is always zero
        value: BigInteger,
        staticCall: Boolean,
    ): ByteArray {
        val ex = executor.clone()
        ex.callData = CallData(caller.hex(), value.u256(), receipt.hex(), CallType.CALL, input.hex())

        if (staticCall) {
            // since static call will not modify states, no needs to merge
            ex.backend = ex.backend.createChild()
            ex.backend.staticCall = true
        }
        return ex.executeInternal()
    }

    override fun delegate(
        originCaller: ByteArray,
        originContract: ByteArray,
        delegateAddr: ByteArray,
        input: ByteArray
    ): ByteArray {
        val ex = executor.clone()
        ex.callData = CallData(
            originCaller.hex(), Uint256.ZERO, originContract.hex(),
            CallType.DELEGATE, input.hex(), delegateAddr.hex()
        )
        return ex.executeInternal()
    }

    override fun drop(address: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun create(caller: ByteArray, value: BigInteger, createCode: ByteArray): ByteArray {
        val ex = executor.clone()
        val addr = HashUtil.calcNewAddr(caller, backend.getNonce(caller.hex()).bytes()).hex()
        ex.callData = CallData(caller.hex(), value.u256(), addr, CallType.CREATE, createCode.hex())
        ex.executeInternal()
        return addr.bytes
    }

    override fun log(contract: ByteArray, data: ByteArray, topics: List<ByteArray>) {
        executor.logs.add(LogInfo(contract.hex(), topics.map { it.hex().h256() }, data.hex()))
    }
}