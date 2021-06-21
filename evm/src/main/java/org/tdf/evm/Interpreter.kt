package org.tdf.evm

import java.math.BigInteger

internal val emptyByteArray = ByteArray(0)
internal val emptyAddress = ByteArray(20)

class EvmContext(
    // transaction.sender
    val origin: ByteArray = emptyAddress,

    // current block number
    val number: Long = 0,

    // chain id
    val chainId: Long = 0,
    val timestamp: Long = 0,
    val difficulty: BigInteger = BigInteger.ZERO,

    // gas limit in block
    val blockGasLimit: Long = 0,

    // gas limit in transaction
    val txGasLimit: Long = 0,
    // gas price
    val gasPrice: BigInteger = BigInteger.ZERO
)

class EvmCallData(
    val caller: ByteArray = emptyAddress,
    val receipt: ByteArray = emptyAddress,
    val value: BigInteger = BigInteger.ZERO,
    val input: ByteArray = emptyByteArray,
    val code: ByteArray = emptyByteArray,
)

interface EvmHost {
    val digest: Digest

    // get balance of address, address.length = 20
    fun getBalance(address: ByteArray): BigInteger
    fun setBalance(address: ByteArray, balance: BigInteger)

    fun getStorage(address: ByteArray, key: ByteArray): ByteArray
    fun setStorage(address: ByteArray, key: ByteArray, value: ByteArray)

    fun getCode(addr: ByteArray): ByteArray

    fun call(
        caller: ByteArray,
        receipt: ByteArray,
        value: BigInteger,
        input: ByteArray,
        delegate: Boolean = false
    ): ByteArray

    fun drop(address: ByteArray)
}

enum class Status {
    READY,
    RUNNING,
    STOP,
    REVERTED
}

class Interpreter(val host: EvmHost, val ctx: EvmContext, val callData: EvmCallData) {
    var pc: Int = 0
    private val stack = StackImpl()
    private val memory = MemoryImpl()
    var ret: ByteArray = emptyByteArray

    var status: Status = Status.READY
        private set

    fun execute(data: EvmCallData) {
        while (pc < data.code.size) {

            when (val op = data.code[pc].toUByte().toInt()) {
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
                OpCodes.ADDRESS -> stack.push(callData.receipt)
                OpCodes.BALANCE -> {
                    val balance = host.getBalance(stack.popAsAddress())
                    stack.push(balance)
                }
                OpCodes.ORIGIN -> stack.push(ctx.origin)
                OpCodes.CALLER -> stack.push(callData.caller)
                OpCodes.CALLVALUE -> stack.push(callData.value)
                OpCodes.CALLDATALOAD -> stack.callDataLoad(callData.input)
                OpCodes.CALLDATASIZE -> stack.pushInt(callData.input.size)
                OpCodes.CALLDATACOPY -> stack.dataCopy(memory, callData.input)
                OpCodes.CODESIZE -> stack.pushInt(callData.code.size)
                OpCodes.CODECOPY -> stack.dataCopy(memory, callData.code)
                OpCodes.GASPRICE -> stack.push(ctx.gasPrice)
                OpCodes.EXTCODESIZE -> {
                }
                OpCodes.POP -> stack.drop()
                OpCodes.MLOAD -> stack.mload(memory)
                OpCodes.MSTORE -> {
                    stack.mstore(memory)
                }
                OpCodes.MSTORE8 -> stack.mstore8(memory)

                OpCodes.RETURN -> {
                    ret = stack.ret(memory)
                    break
                }
                OpCodes.REVERT -> {
                    break
                }
                // push(code[pc+1:pc+1+n])
                in OpCodes.PUSH1..OpCodes.PUSH32 -> {
                    val n = Math.min(op - OpCodes.PUSH1 + 1, data.code.size - pc - 1)
                    stack.push(data.code, pc + 1, n)
                    pc += n
                    pc++
                    continue
                }
                in OpCodes.DUP1..OpCodes.DUP16 -> stack.dup(op - OpCodes.DUP1 + 1)
                in OpCodes.SWAP1..OpCodes.SWAP16 -> stack.swap(op - OpCodes.SWAP1 + 1)
            }

            pc++
        }
    }
}