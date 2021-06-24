package org.tdf.sunflower.vm

import org.tdf.common.types.Uint256
import org.tdf.common.util.HashUtil
import org.tdf.common.util.HexBytes
import org.tdf.evm.Digest
import org.tdf.evm.EvmHost
import org.tdf.sunflower.facade.RepositoryReader
import java.math.BigInteger

val sha3 = Digest {
        src: ByteArray, srcPos: Int, srcLen: Int,
        dst: ByteArray, dstPos: Int ->
    HashUtil.sha3(src, srcPos, srcLen, dst, dstPos)
}

class EvmHostImpl(private val backend: Backend, private val rd: RepositoryReader): EvmHost {
    override val digest: Digest
        get() = sha3

    override fun getBalance(address: ByteArray): BigInteger {
        return backend.getBalance(HexBytes.fromBytes(address)).value
    }

    override fun setBalance(address: ByteArray, balance: BigInteger) {
        backend.setBalance(HexBytes.fromBytes(address), Uint256.Companion.of(balance))
    }

    override fun getStorage(address: ByteArray, key: ByteArray): ByteArray {
        return backend.dbGet(
            HexBytes.fromBytes(address),
            HexBytes.fromBytes(key)
        ).bytes
    }

    override fun setStorage(address: ByteArray, key: ByteArray, value: ByteArray) {
        backend.dbSet(
            HexBytes.fromBytes(address),
            HexBytes.fromBytes(key),
            HexBytes.fromBytes(value)
        )
    }

    override fun getCode(addr: ByteArray): ByteArray {
        return backend.getCode(HexBytes.fromBytes(addr)).bytes
    }

    override fun call(
        caller: ByteArray,
        receipt: ByteArray,
        input: ByteArray,
        value: BigInteger,
        delegate: Boolean
    ): ByteArray {
        TODO("Not yet implemented")
    }

    override fun drop(address: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun create(caller: ByteArray, value: BigInteger, createCode: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }

    override fun log(contract: ByteArray, data: ByteArray, topics: List<ByteArray>) {
    }
}