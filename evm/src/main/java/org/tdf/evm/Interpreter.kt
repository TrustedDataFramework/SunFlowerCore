package org.tdf.evm

import java.io.PrintStream
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

/**
 * for transaction deploy
 */
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
        input: ByteArray,
        value: BigInteger = BigInteger.ZERO,
        delegate: Boolean = false
    ): ByteArray

    fun drop(address: ByteArray)

    /**
     * create contract, return the address of new contract
     */
    fun create(caller: ByteArray, value: BigInteger, createCode: ByteArray): ByteArray
}

class Interpreter(
    val host: EvmHost,
    val ctx: EvmContext,
    val callData: EvmCallData,
    private val vmLog: PrintStream? = null
) {
    var pc: Int = 0
    private val stack = StackImpl()
    private val memory = MemoryImpl()
    var ret: ByteArray = emptyByteArray

    var reverted: Boolean = false
        private set

    var op: Int = 0

    private var memOff: Long = 0
    private var memLen: Long = 0

    private fun jump(dst: Long) {
        if (dst < 0 || dst >= callData.code.size)
            throw RuntimeException("jump destination overflow")
        if (callData.code[dst.toInt()].toUByte().toInt() != OpCodes.JUMPDEST)
            throw RuntimeException("jump destination is not JUMPDEST")
        this.pc = dst.toInt()
    }


    fun execute() {
        logInfo()

        while (pc < callData.code.size) {
            op = callData.code[pc].toUByte().toInt()

            beforeExecute()

            when (op) {
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
                    val code = host.getCode(stack.popAsAddress())
                    stack.pushInt(code.size)
                }
                OpCodes.EXTCODECOPY -> {
                    val code = host.getCode(stack.popAsAddress())
                    stack.dataCopy(memory, code)
                }
                OpCodes.BLOCKHASH, OpCodes.COINBASE -> {
                    throw RuntimeException("unsupported op code")
                }
                OpCodes.TIMESTAMP -> {
                    stack.pushLong(ctx.timestamp)
                }
                OpCodes.NUMBER -> {
                    stack.pushLong(ctx.number)
                }
                OpCodes.DIFFICULTY -> {
                    stack.push(ctx.difficulty)
                }
                OpCodes.GASLIMIT -> {
                    stack.pushLong(ctx.blockGasLimit)
                }
                OpCodes.POP -> stack.drop()
                OpCodes.MLOAD -> stack.mload(memory)
                OpCodes.MSTORE -> {
                    stack.mstore(memory)
                }
                OpCodes.MSTORE8 -> stack.mstore8(memory)
                OpCodes.SLOAD -> {
                    stack.push(
                        host.getStorage(
                            callData.receipt,
                            stack.popAsByteArray()
                        )
                    )
                }
                OpCodes.SSTORE -> {
                    host.setStorage(
                        callData.receipt,
                        stack.popAsByteArray(), stack.popAsByteArray()
                    )
                }
                OpCodes.JUMP -> {
                    jump(stack.popUnsignedInt())
                    continue
                }
                OpCodes.JUMPI -> {
                    val dst = stack.popUnsignedInt()
                    val cond = stack.popUnsignedInt() != 0L
                    if (cond) {
                        jump(dst)
                        continue
                    }
                }
                OpCodes.JUMPDEST -> {

                }
                OpCodes.PC -> {
                    stack.pushInt(pc)
                }

                OpCodes.RETURN -> {
                    ret = stack.ret(memory)
                    break
                }
                OpCodes.REVERT -> {
                    break
                }
                // push(code[pc+1:pc+1+n])
                in OpCodes.PUSH1..OpCodes.PUSH32 -> {
                    val n = Math.min(op - OpCodes.PUSH1 + 1, callData.code.size - pc - 1)
                    stack.push(callData.code, pc + 1, n)
                    pc += n
                }
                in OpCodes.DUP1..OpCodes.DUP16 -> stack.dup(op - OpCodes.DUP1 + 1)
                in OpCodes.SWAP1..OpCodes.SWAP16 -> stack.swap(op - OpCodes.SWAP1 + 1)
            }

            pc++
        }
    }

    fun ByteArray.hex(): String {
        return this.joinToString("") {
            java.lang.String.format("%02x", it)
        }
    }

    private fun logInfo() {
        vmLog?.let {
            it.println("go pc = $pc input = ${callData.input.hex()} code size = ${callData.code.size}")
            it.println("code = ${callData.code.hex()}")
        }
    }

    private fun beforeExecute() {
        vmLog?.let {
            it.println("before execute op ${OpCodes.nameOf(op)} pc = $pc")

            if (op >= OpCodes.PUSH1 && op <= OpCodes.PUSH32) {
                var n = op - OpCodes.PUSH1 + 1
                val start = pc + 1
                val max = callData.code.size - start
                if (n > max) {
                    it.println("push$n overflow, roll back to push$max")
                    n = max
                }
                it.println("push ${callData.code.sliceArray(start until start + n).hex()} into stack")
                return
            }

            when (op) {
                OpCodes.ADDRESS -> it.println("push address = ${callData.receipt.hex()}")
                OpCodes.SSTORE -> {
                    val key = stack.back(0)
                    val value = stack.back(1)
                    it.println("sstore key = ${key.hex()}, value = ${value.hex()}")
                }
                OpCodes.SLOAD -> {
                    val loc = stack.back(0)
                    val value = host.getStorage(callData.receipt, loc)
                    it.println("sload key = ${loc.hex()}, value = ${value.hex()}")
                }
                OpCodes.CODECOPY -> {
                    memOff = stack.backUnsignedInt()
                    val codeOff = stack.backUnsignedInt(1)
                    memLen = stack.backUnsignedInt(2)

                    it.println(
                        "codecopy into memory, mem[$memOff:$memOff+$memLen] = code[$codeOff:$codeOff+$memLen] mem.cap = ${memory.size} value = ${
                            callData.code.sliceArray(
                                codeOff.toInt() until codeOff.toInt() + memLen.toInt()
                            ).hex()
                        }"
                    )
                }
                OpCodes.MSTORE -> {
                    memOff = stack.backUnsignedInt()
                    val value = stack.back(1).hex()

                    it.println("mstore $value into memory, mem offset = $memOff mem.cap = ${memory.size}")
                }
                OpCodes.CALLVALUE -> {
                    it.println("callvalue ${callData.value} into stack")
                }
                OpCodes.CALLDATASIZE -> {
                    it.println("calldatasize ${callData.input.size} into stack")
                }
                OpCodes.CALLDATALOAD -> {
                    val off = stack.backUnsignedInt()
                    if (off < 0 || off + 32 > callData.input.size) {
                        it.println("call data load overflows")
                    }
                    val dat = getData(callData.input, off, 32)
                    it.println("calldataload data = ${dat.hex()}")
                }
                OpCodes.JUMPI -> {
                    if (stack.backUnsignedInt(1) == 0L) {
                        it.println("jumpi cond = 0, no jump")
                    } else {
                        it.println("jumpi cond = 1, jump to ${stack.backUnsignedInt(1)}")
                    }
                }
                OpCodes.JUMPDEST -> {
                }
                OpCodes.RETURN -> {
                    val off = if (stack.backUnsignedInt() < 0) {
                        0
                    } else {
                        stack.backUnsignedInt().toInt()
                    }
                    val size = if (stack.backUnsignedInt(1) < 0) {
                        0
                    } else {
                        stack.backUnsignedInt(1).toInt()
                    }
                    val value = ByteArray(size)
                    memory.read(off, value)
                    it.println("return offset = $off size = $size value = ${value.hex()}")
                }
            }
        }
    }

    private fun getData(input: ByteArray, off: Long, len: Long): ByteArray {
        val offInt = unsignedMin(off, input.size.toLong()).toInt()
        val lenInt = Math.min(32, input.size - offInt)
        val r = ByteArray(lenInt)
        System.arraycopy(input, offInt, r, 0, lenInt)
        return r
    }

    private fun unsignedMin(x: Long, y: Long): Long {
        return if (x + Long.MIN_VALUE < y + Long.MIN_VALUE) {
            x
        } else {
            y
        }
    }
}