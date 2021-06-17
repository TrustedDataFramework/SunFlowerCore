package org.tdf.evm

import java.math.BigInteger

interface EvmHost {
    val digest: Digest

    // get balance of address, address.length = 20
    fun balanceOf(address: ByteArray): BigInteger

    // tx origin
    val origin: ByteArray
    // block number
    val number: BigInteger
    val chainId: Long
    val timestamp: Long

    val difficulty: BigInteger
        get() = BigInteger.ZERO

    val blockGasLimit: Long
    val txGasLimit: Long

    fun getStorage(address: ByteArray, key: ByteArray): ByteArray
    fun setStorage(address: ByteArray, key: ByteArray, value: ByteArray)

    fun getCode(address: ByteArray): ByteArray
    fun setCode(address: ByteArray, code: ByteArray)


    fun getNonce(address: ByteArray): BigInteger
    fun setNonce(address: ByteArray, n: BigInteger)

    fun drop(address: ByteArray)
}

class Interpreter(val code: ByteArray, val host: EvmHost) {
    var pc: Int = 0
    var steps: Int = 0
    private val stack = StackImpl()
    private val memory = MemoryImpl()

    fun execute() {

        while (true) {

            when (val op = code[pc].toUByte().toInt()) {
                OpCodes.STOP -> break
                OpCodes.ADD -> stack.add()
                OpCodes.MUL -> stack.mul()
                OpCodes.SUB -> stack.sub()
                OpCodes.DIV -> stack.div()
                OpCodes.SDIV -> stack.signedDiv()
                OpCodes.MOD -> stack.mod()
                OpCodes.SMOD -> stack.signedMod()
                OpCodes.ADDMOD -> stack.addMod()
                OpCodes.MULMOD -> stack.mulMod()
                OpCodes.EXP -> stack.exp()
                OpCodes.SIGNEXTEND -> stack.signExtend()
                OpCodes.LT -> stack.lt()
                OpCodes.GT -> stack.gt()
                OpCodes.SLT -> stack.slt()
                OpCodes.SGT -> stack.sgt()
                OpCodes.EQ -> stack.eq()
                OpCodes.ISZERO -> stack.isZero()
                OpCodes.AND -> stack.and()
                OpCodes.OR -> stack.or()
                OpCodes.XOR -> stack.xor()
                OpCodes.NOT -> stack.not()
                OpCodes.BYTE -> stack.byte()
                OpCodes.SHL -> stack.shl()
                OpCodes.SHR -> stack.shr()
                OpCodes.SAR -> stack.sar()
                OpCodes.SHA3 -> stack.sha3(memory, host.digest)
                OpCodes.POP -> stack.drop()
                OpCodes.MLOAD -> stack.mload(memory)
                OpCodes.MSTORE -> stack.mstore(memory)
                OpCodes.MSTORE8 -> stack.mstore8(memory)

                in OpCodes.PUSH1..OpCodes.PUSH32 -> stack.pushZero()
                in OpCodes.DUP1..OpCodes.DUP16 -> stack.dup(op - OpCodes.DUP1 + 1)
                in OpCodes.SWAP1..OpCodes.SWAP16 -> stack.swap(op - OpCodes.SWAP1 + 1)
            }
        }
    }
}