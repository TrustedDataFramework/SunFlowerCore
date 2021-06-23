package org.tdf.evm

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.spongycastle.util.encoders.Hex
import org.tdf.common.util.ByteUtil
import org.tdf.common.util.HashUtil
import org.tdf.common.util.HexBytes
import java.math.BigInteger

val ZERO_ADDRESS = ByteArray(20)

val sha3 = Digest {
        src: ByteArray, srcPos: Int, srcLen: Int,
        dst: ByteArray, dstPos: Int ->
    HashUtil.sha3(src, srcPos, srcLen, dst, dstPos)
}

class MemAccount(
    var nonce: Long = 0,
    var balance: BigInteger = BigInteger.ZERO,
    val storage: MutableMap<HexBytes, HexBytes> = mutableMapOf(),
    var code: ByteArray = ByteUtil.EMPTY_BYTE_ARRAY
)


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

    override fun getCode(addr: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }

    override fun call(
        caller: ByteArray, receipt: ByteArray, input: ByteArray, value: BigInteger, delegate: Boolean
    ): ByteArray {
        val contract = getOrCreate(receipt)
        if(contract.code.isEmpty())
            throw RuntimeException("not a contract account")

        val callData = EvmCallData(caller, receipt, value, input, contract.code)

        val interpreter = Interpreter(
            this,
            EvmContext(),
            callData
        )

        interpreter.execute()
        return interpreter.ret
    }

    private fun getOrCreate(address: ByteArray): MemAccount {
        val r = accounts[HexBytes.fromBytes(address)]
        if(r != null)
            return r

        val a = MemAccount()
        accounts[HexBytes.fromBytes(address)] = a
        return a
    }

    override fun drop(address: ByteArray) {
    }

    override fun create(caller: ByteArray, value: BigInteger, createCode: ByteArray): ByteArray {
        val cal = getOrCreate(caller)
        val nonce = cal.nonce
        cal.nonce++
        val newAddr = HashUtil.calcNewAddr(caller, ByteUtil.longToBytesNoLeadZeroes(nonce))

        val interpreter = Interpreter(
            this,
                EvmContext(),
                EvmCallData(caller, newAddr, value, emptyByteArray, createCode),
                System.out
        )

        interpreter.execute()
        cal.code = interpreter.ret
        return newAddr
    }
}

@RunWith(JUnit4::class)
class VMTests {

    @Test
    fun test0() {
        val code = intArrayOf(
            OpCodes.PUSH1, 10, OpCodes.PUSH1, 0, OpCodes.MSTORE, OpCodes.PUSH1, 32, OpCodes.PUSH1, 0, OpCodes.RETURN
        ).map { it.toByte() }.toByteArray()

        val ctx = EvmContext()
        val data = EvmCallData(code = code)
        val mock = MockEvmHost()
        val executor = Interpreter(mock, ctx, data)

        executor.execute()
        assert(BigInteger(1, executor.ret).intValueExact() == 10)
    }

    @Test
    fun testOwnerContract() {
        val objectMapper: ObjectMapper = jacksonObjectMapper()
        val codeJson = TestUtil.readClassPathFile("contracts/owner.json")
        val node = objectMapper.readValue(codeJson, JsonNode::class.java)

        val code = Hex.decode(node.get("object").asText())

        val mock = MockEvmHost()

        mock.create(ZERO_ADDRESS, BigInteger.ZERO, code)
    }
}