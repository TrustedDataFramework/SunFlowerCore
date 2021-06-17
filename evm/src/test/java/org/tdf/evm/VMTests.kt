package org.tdf.evm

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.tdf.common.util.ByteUtil
import org.tdf.common.util.HashUtil
import org.tdf.common.util.HexBytes
import java.math.BigInteger

val ZERO_ADDRESS = ByteArray(20)

val sha3 = Digest { src: ByteArray, srcPos: Int, srcLen: Int,
                    dst: ByteArray, dstPos: Int ->
    HashUtil.sha3(src, srcPos, srcLen, dst, dstPos)
}

class MemAccount(
    var nonce: Long = 0,
    var balance: BigInteger = BigInteger.ZERO,
    val storage: MutableMap<HexBytes, HexBytes> = mutableMapOf(),
    var code: ByteArray = ByteUtil.EMPTY_BYTE_ARRAY
)

class EvmContextImpl(
    override val origin: ByteArray = ZERO_ADDRESS,
    override val number: Long = 0,
    override val chainId: Long = 0,
    override val timestamp: Long = 0,
    override val difficulty: BigInteger = BigInteger.ZERO,
    override val blockGasLimit: Long = Long.MAX_VALUE,
    override val txGasLimit: Long = Long.MAX_VALUE
) : EvmContext

class MockCallData(
    override val caller: ByteArray = ZERO_ADDRESS,
    override val receipt: ByteArray = ZERO_ADDRESS,
    override val value: BigInteger = BigInteger.ZERO,
    override val input: ByteArray = ByteUtil.EMPTY_BYTE_ARRAY,
    override val code: ByteArray = ByteUtil.EMPTY_BYTE_ARRAY
) : EvmCallData {

}

class MockEvmHost : EvmHost {
    override val digest: Digest = sha3
    private val accounts: MutableMap<HexBytes, MemAccount> = mutableMapOf()

    override fun getBalance(address: ByteArray): BigInteger {
        return accounts[HexBytes.fromBytes(address)]?.balance ?: BigInteger.ZERO
    }

    override fun setBalance(address: ByteArray, balance: BigInteger) {
        val account = accounts[HexBytes.fromBytes(address)] ?: MemAccount()
        account.balance = balance
        accounts[HexBytes.fromBytes(address)] = account
    }

    override fun getStorage(address: ByteArray, key: ByteArray): ByteArray {
        return accounts[HexBytes.fromBytes(address)]
            ?.storage?.let {
                it[HexBytes.fromBytes(key)]
            }?.bytes ?: HexBytes.EMPTY_BYTES
    }

    override fun setStorage(address: ByteArray, key: ByteArray, value: ByteArray) {
        val account = accounts[HexBytes.fromBytes(address)] ?: MemAccount()
        account.storage[HexBytes.fromBytes(key)] = HexBytes.fromBytes(value)
        accounts[HexBytes.fromBytes(address)] = account
    }

    override fun call(caller: ByteArray, receipt: ByteArray, value: BigInteger, input: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }

    override fun delegateCall(caller: ByteArray, receipt: ByteArray, value: BigInteger, input: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }

    override fun drop(address: ByteArray) {
        TODO("Not yet implemented")
    }
}

@RunWith(JUnit4::class)
class VMTests {

    @Test
    fun test0() {
        val code = intArrayOf(
            OpCodes.PUSH1, 10, OpCodes.PUSH1, 0, OpCodes.MSTORE, OpCodes.PUSH1, 32, OpCodes.PUSH1, 0, OpCodes.RETURN
        ).map { it.toByte() }.toByteArray()

        val ctx = EvmContextImpl()
        val mock = MockEvmHost()
        val executor = Interpreter(mock)
        val data = MockCallData(code = code)

        executor.execute(data)
        assert(BigInteger(1, executor.ret).intValueExact() == 10)
    }
}