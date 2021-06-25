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

    fun getStorage(address: ByteArray, key: ByteArray): ByteArray
    fun setStorage(address: ByteArray, key: ByteArray, value: ByteArray)

    fun getCode(addr: ByteArray): ByteArray

    fun call(
        caller: ByteArray,
        receipt: ByteArray,
        input: ByteArray,
        // when static call = true, value is always zero
        value: BigInteger = BigInteger.ZERO,
        staticCall: Boolean = false
    ): ByteArray

    fun delegate(
        originCaller: ByteArray,
        originContract: ByteArray,
        delegateAddr: ByteArray,
        input: ByteArray,
    ): ByteArray

    fun drop(address: ByteArray)

    /**
     * create contract, return the address of new contract
     */
    fun create(caller: ByteArray, value: BigInteger, createCode: ByteArray): ByteArray

    fun log(contract: ByteArray, data: ByteArray, topics: List<ByteArray>)
}

class Interpreter(
    val host: EvmHost,
    val ctx: EvmContext,
    val callData: EvmCallData,
    private val vmLog: PrintStream? = null,
    maxStackSize: Int = Int.MAX_VALUE,
    maxMemorySize: Int = Int.MAX_VALUE
) {
    var pc: Int = 0
    private val stack = StackImpl(maxStackSize)
    private val memory = MemoryImpl(maxMemorySize)
    var ret: ByteArray = emptyByteArray

    var reverted: Boolean = false
        private set

    var op: Int = 0

    private var memOff: Long = 0
    private var memLen: Long = 0
    private var key: ByteArray = emptyByteArray

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
                OpCodes.STOP -> {
                    afterExecute()
                    break
                }
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
                    val balance = host.getBalance(stack.popAddress())
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
                    val code = host.getCode(stack.popAddress())
                    stack.pushInt(code.size)
                }
                OpCodes.EXTCODECOPY -> {
                    val code = host.getCode(stack.popAddress())
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
                            stack.popBytes()
                        )
                    )
                }
                OpCodes.SSTORE -> {
                    host.setStorage(
                        callData.receipt,
                        stack.popBytes(), stack.popBytes()
                    )
                }
                OpCodes.JUMP -> {
                    jump(stack.popU32())
                    afterExecute()
                    continue
                }
                OpCodes.JUMPI -> {
                    val dst = stack.popU32()
                    val cond = stack.popU32() != 0L
                    if (cond) {
                        jump(dst)
                        afterExecute()
                        continue
                    } else {
                        pc++
                        afterExecute()
                        continue
                    }
                }
                OpCodes.JUMPDEST -> {

                }
                OpCodes.PC -> {
                    stack.pushInt(pc)
                }

                OpCodes.RETURN -> {
                    ret = stack.popMemory(memory)
                    afterExecute()
                    break
                }
                OpCodes.REVERT -> {
                    afterExecute()
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

                in OpCodes.LOG0..OpCodes.LOG4 -> {
                    val n = op - OpCodes.LOG0
                    val topics = mutableListOf<ByteArray>()
                    val data = stack.popMemory(memory)
                    for (i in 0 until n) {
                        topics.add(stack.popBytes())
                    }
                    host.log(callData.receipt, data, topics)
                }
                OpCodes.GAS -> {
                    stack.pushZero()
                }
                OpCodes.STATICCALL, OpCodes.CALL, OpCodes.DELEGATECALL -> {
                    call(op)
                }
            }

            afterExecute()
            pc++
        }
    }

    fun ByteArray.hex(start: Int = 0, end: Int = this.size): String {
        return "0x" + this.sliceArray(start until end).joinToString("") {
            java.lang.String.format("%02x", it)
        }
    }


    fun ByteArray.bnHex(): String {
        return BigInteger(1, this).hex()
    }

    fun BigInteger.hex(): String {
        return "0x" + this.toString(16)
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
                it.println("push ${callData.code.hex(start, start + n)} into stack")
                return
            }

            when (op) {
                OpCodes.MLOAD -> {
                    val off = stack.backBigInt()
                    it.println("mload off = $off stack.push(mem[$off:$off+32]), mem.cap = ${memory.size}")
                }
                OpCodes.ADDRESS -> it.println("push address = ${callData.receipt.hex()}")
                OpCodes.SSTORE -> {
                    key = stack.back(0)
                    val value = stack.back(1)
                    it.println("sstore key = ${key.hex()}, value = ${value.hex()}")
                }
                OpCodes.SLOAD -> {
                    val loc = stack.back(0)
                    val value = host.getStorage(callData.receipt, loc)
                    val value32 = if (value.isEmpty()) {
                        ByteArray(32)
                    } else {
                        value
                    }
                    it.println("sload key = ${loc.hex()}, value = ${value32.hex()}")
                }
                OpCodes.CODECOPY -> {
                    memOff = stack.backU32()
                    val codeOff = stack.backU32(1)
                    memLen = stack.backU32(2)

                    it.println(
                        "codecopy into memory, mem[$memOff:$memOff+$memLen] = code[$codeOff:$codeOff+$memLen] mem.cap = ${memory.size} value = ${
                            callData.code.hex(
                                codeOff.toInt(), codeOff.toInt() + memLen.toInt()
                            )
                        }"
                    )
                }
                OpCodes.MSTORE -> {
                    memOff = stack.backU32()
                    val value = stack.back(1).bnHex()

                    it.println("mstore $value into memory, mem offset = $memOff mem.cap = ${memory.size}")
                }
                OpCodes.CALLVALUE -> {
                    it.println("callvalue ${callData.value} into stack")
                }
                OpCodes.CALLDATASIZE -> {
                    it.println("calldatasize ${callData.input.size} into stack")
                }
                OpCodes.CALLDATALOAD -> {
                    val off = stack.backU32()
                    if (off < 0 || off + 32 > callData.input.size) {
                        it.println("call data load overflows")
                    }
                    val dat = getData(callData.input, off, 32)
                    it.println("calldataload data off = ${off} len = 32, input len = ${callData.input.size}, value = ${dat.hex()}")
                }
                OpCodes.JUMPI -> {
                    if (stack.backU32(1) == 0L) {
                        it.println("jumpi cond = 0, no jump")
                    } else {
                        it.println("jumpi cond = 1, jump to ${stack.backU32(0)}")
                    }
                }
                OpCodes.JUMP -> it.println(
                    "jump to dest ${stack.backBigInt()} op = ${
                        OpCodes.nameOf(
                            callData.code[stack.backU32().toInt()].toUByte().toInt()
                        )
                    }"
                )
                OpCodes.JUMPDEST -> {
                }
                in OpCodes.LOG0..OpCodes.LOG4 -> {
                    it.println("log${op - OpCodes.LOG0} mem off = ${stack.backBigInt(0)} len = ${stack.backBigInt(1)}, mem.cap = ${memory.size}")
                }
                OpCodes.RETURN -> {
                    val off = if (stack.backU32() < 0) {
                        0
                    } else {
                        stack.backU32().toInt()
                    }
                    val size = if (stack.backU32(1) < 0) {
                        0
                    } else {
                        stack.backU32(1).toInt()
                    }
                    val value = ByteArray(size)
                    memory.read(off, value)
                    it.println("return offset = $off size = $size value = ${value.hex()}")
                }
            }
        }
    }

    fun afterExecute() {
        vmLog?.let {
            it.println("after execute op ${OpCodes.nameOf(op)} pc = $pc")

            val stackData = "[" + (0 until stack.size).map { bn -> stack.get(bn).hex() }.joinToString(",") + "]"
            it.println("stack = $stackData")

            when (op) {
                OpCodes.CALL, OpCodes.STATICCALL, OpCodes.DELEGATECALL -> {
                    it.println("${OpCodes.nameOf(op)} success memOff = $memOff menLen = $memLen ret = ${memory.data.hex(memOff.toInt(), (memOff + memLen).toInt())}")
                }
                OpCodes.SSTORE -> it.println(
                    "sstore success key = ${key.hex()}, value = ${
                        host.getStorage(
                            callData.receipt,
                            key
                        ).hex()
                    }"
                )
                OpCodes.CODECOPY -> it.println(
                    "codecopy success mem.cap = ${memory.size}, mem[${memOff}:${memOff}+${memLen}] = ${
                        memory.copy(
                            memOff.toInt(),
                            (memOff + memLen).toInt()
                        ).hex()
                    }"
                )
                OpCodes.MSTORE -> it.println(
                    "mstore success, mem[${memOff}:${memOff}+32] = ${
                        memory.copy(
                            memOff.toInt(),
                            (memOff + 32).toInt()
                        ).hex()
                    } mem.cap = ${memory.size}"
                )
                OpCodes.JUMPI -> {
                }
                OpCodes.RETURN -> {
                }
            }
        }
    }

    private fun getData(input: ByteArray, off: Long, len: Long): ByteArray {
        val offInt = unsignedMin(off, input.size.toLong()).toInt()
        val lenInt = unsignedMin(len, (input.size - offInt).toLong()).toInt()
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

    fun call(op: Int) {
        val temp = stack.popBigInt()
        val addr = stack.popAddress()
        val value = if(op == OpCodes.CALL) { stack.popBigInt() } else { BigInteger.ZERO }
        val inOff = stack.popU32()
        val inSize = stack.popU32()
        val retOff = stack.popU32()
        val retSize = stack.popU32()

        memOff = retOff
        memLen = retSize

        memory.resize(inOff, inSize)

        val input = memory.copy(inOff.toInt(), (inOff + inSize).toInt())
        var ret = emptyByteArray

        vmLog?.println("${OpCodes.nameOf(op).lowercase()} address ${addr.hex()} args = ${input.hex()} value = $value retOffset = $retOff retSize = $retSize temp = $temp mem.cap = ${memory.size}")

        when(op) {
            OpCodes.CALL, OpCodes.STATICCALL -> {
                ret = host.call(callData.receipt, addr, input, value, op == OpCodes.STATICCALL)
            }
            OpCodes.DELEGATECALL -> {
                ret = host.delegate(callData.caller, callData.receipt, addr, input)
            }
        }


        memory.resize(retOff, retSize)
        if(retSize.toInt() != ret.size)
            throw RuntimeException("unexpected return size")
        memory.write(retOff.toInt(), ret, 0, retSize.toInt())
        stack.push(temp)

    }
}