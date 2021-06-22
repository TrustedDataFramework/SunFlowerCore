package org.tdf.evm

import org.tdf.evm.SlotUtils.*
import java.math.BigInteger
import java.util.*

fun interface Digest {
    // dst[dstPos:] = sha3(src[srcPos:srcPos+srcLen])
    fun digest(src: ByteArray, srcPos: Int, srcLen: Int, dst: ByteArray, dstPos: Int)
}

interface Stack {
    val size: Int
    fun backUnsignedInt(i: Int = 0): Long
    fun back(i: Int = 0): ByteArray

    fun popAsAddress(): ByteArray

    // memory size for m
    fun push(buf: ByteArray, offset: Int = 0, len: Int = buf.size)

    fun push(n: IntArray, offset: Int = 0)

    fun push(n: MutableBigInteger)

    fun push(b: Boolean) {
        if (b) pushOne() else pushZero()
    }

    fun pushZero()

    fun pushOne()

    /**
     * push a value to stack
     */
    fun push(n: BigInteger)

    /**
     * push a long value to stack
     */
    fun pushLong(n: Long)

    /**
     * push a int value to stack
     */
    fun pushInt(n: Int)

    fun drop()

    /**
     * pop top into buffer
     */
    fun pop(buf: IntArray, offset: Int = 0)


    /**
     * pop as byte array
     */
    fun popAsByteArray(): ByteArray

    /**
     * pop as biginteger
     */
    fun popBigInt(signed: Boolean = false): BigInteger

    /**
     * pop as unsigned integer, return -1 if overflowed
     */
    fun popUnsignedInt(): Long

    fun popIntExact(): Int {
        val n = popUnsignedInt()
        if (n < 0)
            throw RuntimeException("integer overflow")
        return n.toInt()
    }

    // arithmetic, unsigned
    fun add()
    fun sub()
    fun mul()
    fun mulMod()
    fun div()
    fun mod()
    fun addMod()
    fun exp()

    // arithmetic, signed
    fun signedDiv()
    fun signedMod()

    // bitwise, comparison
    fun signExtend()
    fun and()
    fun or()
    fun xor()
    fun not()
    fun eq()
    fun lt()
    fun slt()
    fun gt()
    fun sgt()
    fun isZero()
    fun byte()
    fun shl()
    fun shr()
    fun sar()

    fun sha3(mem: Memory, digest: Digest)
    fun callDataLoad(input: ByteArray)
    fun dataCopy(mem: Memory, data: ByteArray)

    fun dup(index: Int)
    fun swap(index: Int)

    // memory access
    fun mstore(mem: Memory)
    fun mstore8(mem: Memory)
    fun mload(mem: Memory)
    fun ret(mem: Memory): ByteArray
}

class StackImpl(private val limit: Int = Int.MAX_VALUE) : Stack {
    override fun backUnsignedInt(i: Int): Long {
        if (i < 0 || i >= size)
            throw RuntimeException("stack underflow")
        val idx = size - 1 - i
        for (j in 0 until SLOT_MAX_INDEX) {
            if (data[idx * SLOT_SIZE + j] != 0) {
                return -1
            }
        }
        return Integer.toUnsignedLong(data[idx * SLOT_SIZE + SLOT_MAX_INDEX])
    }

    override fun back(i: Int): ByteArray {
        if (i < 0 || i >= size)
            throw RuntimeException("stack underflow")
        val r = ByteArray(SLOT_BYTE_ARRAY_SIZE)
        encodeBE(data, i * SLOT_SIZE, r, 0)
        return r
    }

    private fun memSizeByOffAndLen(off: Long, len: Long): Int {
        val i = off + len
        if (off < 0 || len < 0 || i < 0)
            throw RuntimeException("memory access overflow")
        if (i > Int.MAX_VALUE)
            throw RuntimeException("memory access overflow")
        return i.toInt()
    }

    private var top: Int = -SLOT_SIZE

    override var size: Int = 0
        private set

    override fun popAsAddress(): ByteArray {
        val r = ByteArray(20)
        encodeBE(data, top, tempBytes, 0)
        System.arraycopy(tempBytes, SLOT_BYTE_ARRAY_SIZE - 20, r, 0, 20)
        dropNocheck()
        return r
    }

    private var data: IntArray = IntArray(SLOT_SIZE * INITIAL_CAP)

    private var cap: Int = INITIAL_CAP

    private val operand0 = IntArray(SLOT_SIZE * 2)
    private val operandMut0 = MutableBigInteger(operand0)

    private val operand1 = IntArray(SLOT_SIZE * 2)
    private val operandMut1 = MutableBigInteger(operand1)

    private val varSlot = IntArray(SLOT_SIZE * 2)
    private val varMut = MutableBigInteger(varSlot)

    private val divisor = IntArray(SLOT_SIZE * 2)

    private val remSlot = IntArray(SLOT_SIZE * 4)
    private val remMut = MutableBigInteger(remSlot)


    private fun tryGrow() {
        if (size != cap)
            return
        cap *= 2
        val tmp = IntArray(cap * SLOT_SIZE)
        System.arraycopy(data, 0, tmp, 0, data.size)
        this.data = tmp
    }

    override fun push(buf: ByteArray, offset: Int, len: Int) {
        if (len > SLOT_BYTE_ARRAY_SIZE)
            throw RuntimeException("byte array size overflow")
        Arrays.fill(tempBytes, 0)
        System.arraycopy(buf, offset, tempBytes, SLOT_BYTE_ARRAY_SIZE - len, len)
        pushZero()
        decodeBE(tempBytes, 0, data, top)
    }

    override fun push(n: MutableBigInteger) {
        if (size == limit)
            throw RuntimeException("stack overflow")
        tryGrow()
        size++
        top += SLOT_SIZE
        Arrays.fill(data, top, top + SLOT_SIZE, 0)
        trim256(n.value, n.offset, n.intLen, data, top)
    }

    override fun pop(buf: IntArray, offset: Int) {
        if (size == 0)
            throw RuntimeException("stack underflow")
        System.arraycopy(data, top, buf, offset, SLOT_SIZE)
        size--
        top -= SLOT_SIZE
    }

    override fun popAsByteArray(): ByteArray {
        if (size == 0)
            throw RuntimeException("stack underflow")
        val r = ByteArray(SLOT_BYTE_ARRAY_SIZE)
        encodeBE(data, top, r, 0)
        dropNocheck()
        return r
    }

    override fun popBigInt(signed: Boolean): BigInteger {
        if (size == 0)
            throw RuntimeException("stack underflow")

        val neg = (data[top] and SIGN_BIT_MASK) != 0

        if (signed && neg) {
            complement(data, top)
        }

        encodeBE(data, top, tempBytes, 0)
        val r = BigInteger(
            if (signed && neg) {
                -1
            } else {
                1
            }, tempBytes
        )
        size--
        top -= SLOT_SIZE
        return r
    }

    override fun popUnsignedInt(): Long {
        for (i in 0 until SLOT_MAX_INDEX) {
            if (data[top + i] != 0) {
                drop()
                return -1
            }
        }
        val r = data[top + SLOT_MAX_INDEX]
        drop()
        return Integer.toUnsignedLong(r)
    }

    override fun push(n: IntArray, offset: Int) {
        if (size == limit)
            throw RuntimeException("stack overflow")
        tryGrow()
        size++
        top += SLOT_SIZE
        System.arraycopy(n, offset, data, top, SLOT_SIZE)
    }

    private fun pushInternal(n: BigInteger) {
        tryGrow()
        Arrays.fill(tempBytes, 0)
        size++
        top += SLOT_SIZE
        copyFrom(tempBytes, 0, n)
        decodeBE(tempBytes, 0, data, top)
    }

    override fun push(n: BigInteger) {
        if (size == limit)
            throw RuntimeException("stack overflow")
        if (n.signum() >= 0) {
            pushInternal(n)
            return
        }
        pushInternal(n.and(P_MAX))
    }

    override fun pushOne() {
        pushZero()
        data[top + 7] = 1
    }

    override fun pushZero() {
        if (size == limit)
            throw RuntimeException("stack overflow")
        tryGrow()
        size++
        top += SLOT_SIZE
        Arrays.fill(data, top, top + SLOT_SIZE, 0)
    }

    override fun pushLong(n: Long) {
        pushZero()
        data[top + 6] = (n ushr 32).toInt()
        data[top + 7] = n.toInt()
    }

    override fun pushInt(n: Int) {
        pushZero()
        data[top + 7] = n
    }

    override fun add() {
        addInternal()
        size--
        top -= SLOT_SIZE
    }

    override fun mul() {
        popOperand0()
        popOperand1()
        if (operandMut0.isZero || operandMut1.isZero) {
            pushZero()
            return
        }
        varMut.clear()
        operandMut0.multiply(operandMut1, varMut)
        push(varMut)
    }

    override fun mulMod() {
        popOperand0()
        popOperand1()
        varMut.clear()

        if (operandMut0.isZero || operandMut1.isZero) {
            drop()
            pushZero()
            return
        }

        operandMut0.multiply(operandMut1, varMut)
        operandMut0.copyValue(varMut)
        varMut.clear()
        popOperand1()

        if (operandMut1.isZero) {
            pushZero()
            return
        }
        divModInternal(true)
        push(remMut)
    }

    override fun exp() {
        val num = popBigInt()
        val exp = popBigInt()
        push(num.modPow(exp, P_2_256))
    }

    override fun div() {
        popOperand0()
        popOperand1()
        if (operandMut1.isZero) {
            pushZero()
            return
        }
        divModInternal(false)
        push(varMut)
    }

    override fun dup(index: Int) {
        if(index > size)
            throw RuntimeException("stack underflow")
        val src = size - index
        pushZero()
        System.arraycopy(data, src * SLOT_SIZE, data, top, SLOT_SIZE)
    }

    override fun swap(index: Int) {
        if (index >= size)
            throw RuntimeException("stack underflow")
        if (index == size - 1)
            return
        // op0 = top
        System.arraycopy(data, top, operand0, 0, SLOT_SIZE)
        // top = stack[index]
        System.arraycopy(data, index * SLOT_SIZE, data, top, SLOT_SIZE)
        // stack[index] = op0
        System.arraycopy(operand0, 0, data, index * SLOT_SIZE, SLOT_SIZE)
    }

    override fun mstore(mem: Memory) {
        mem.resize(memSizeByOffAndLen(backUnsignedInt(), 32))
        val off = popIntExact()
        if (size == 0)
            throw RuntimeException("stack underflow")
        encodeBE(data, top, tempBytes, 0)
        drop()
        mem.write(off, tempBytes)
    }

    override fun mstore8(mem: Memory) {
        pushInt(0xff)
        and()
        mstore(mem)
    }

    override fun mload(mem: Memory) {
        mem.resize(memSizeByOffAndLen(backUnsignedInt(), backUnsignedInt(1)))
        val off = popIntExact()
        if (size == 0)
            throw RuntimeException("stack underflow")
        mem.read(off, tempBytes)
        pushZero()
        decodeBE(tempBytes, 0, data, top)
    }

    override fun ret(mem: Memory): ByteArray {
        val off = popIntExact()
        val len = popIntExact()
        val r = ByteArray(len)
        mem.read(off, r)
        return r
    }


    private fun addInternal(): Long {
        if (size < 2)
            throw RuntimeException("stack underflow")
        return add(data, top - SLOT_SIZE, data, top, data, top - SLOT_SIZE)
    }

    override fun sub() {
        if (size < 2)
            throw RuntimeException("stack underflow")
        sub(data, top, data, top - SLOT_SIZE, data, top - SLOT_SIZE)
        dropNocheck()
    }

    // perform operation
    // op0 / op1 = varMut
    // op0 = remMut mod op1 when needRem = true
    private fun divModInternal(needRem: Boolean) {
        // clear quotient
        varMut.clear()
        // clear divisor slot
        Arrays.fill(divisor, 0)
        // clear rem
        remMut.clear()
        operandMut0.divideKnuth(operandMut1, varMut, remMut, divisor, needRem)
    }

    private fun popOperand0(carry: Long = 0) {
        Arrays.fill(operand0, 0)
        pop(operand0, SLOT_SIZE)
        operand0[SLOT_MAX_INDEX] = carry.toInt()
        operandMut0.setValue(operand0, operand0.size)
        operandMut0.normalize()
    }

    private fun popOperand1() {
        Arrays.fill(operand1, 0)
        pop(operand1, SLOT_SIZE)
        operandMut1.setValue(operand1, operand1.size)
        operandMut1.normalize()
    }

    override fun addMod() {
        val carry = addInternal()
        drop()
        // pop addition result into operand0
        popOperand0(carry)

        // pop modular into operand1
        popOperand1()
        if (operandMut1.isZero) {
            pushZero()
            return
        }
        divModInternal(true)
        push(remMut)
    }

    override fun and() {
        if (size < 2)
            throw RuntimeException("stack overflow")
        for (i in top - SLOT_SIZE until top) {
            data[i] = data[i] and data[i + SLOT_SIZE]
        }
        size--
        top -= SLOT_SIZE
    }

    override fun or() {
        if (size < 2)
            throw RuntimeException("stack overflow")
        for (i in top - SLOT_SIZE until top) {
            data[i] = data[i] or data[i + SLOT_SIZE]
        }
        size--
        top -= SLOT_SIZE
    }

    override fun xor() {
        if (size < 2)
            throw RuntimeException("stack overflow")
        for (i in top - SLOT_SIZE until top) {
            data[i] = data[i] xor data[i + SLOT_SIZE]
        }
        size--
        top -= SLOT_SIZE
    }

    override fun signedMod() {
        signedDivMod(true)
    }

    override fun signExtend() {
        if (size < 2)
            throw RuntimeException("stack underflow")

        val n = popUnsignedInt()
        if (n < 0 || n >= SLOT_BYTE_ARRAY_SIZE)
            return

        val bytesLong = n + 1
        val bytes = bytesLong.toInt()

        encodeBE(data, top, tempBytes, 0)

        if (tempBytes[SLOT_BYTE_ARRAY_SIZE - bytes] >= 0) {
            Arrays.fill(tempBytes, 0, SLOT_BYTE_ARRAY_SIZE - bytes, 0)
            decodeBE(tempBytes, 0, data, top)
            return
        }

        Arrays.fill(tempBytes, 0, SLOT_BYTE_ARRAY_SIZE - bytes, 0xff.toByte())
        decodeBE(tempBytes, 0, data, top)
    }

    override fun signedDiv() {
        signedDivMod(false)
    }

    private fun signedDivMod(rem: Boolean) {
        if (size < 2)
            throw RuntimeException("stack underflow")

        // special case any / 1 = any, any % 1 = 0
        if (isOne(data, top - SLOT_SIZE)) {
            if (rem) {
                Arrays.fill(data, top - SLOT_SIZE, top, 0)
            } else {
                System.arraycopy(data, top, data, top - SLOT_SIZE, SLOT_SIZE)
            }
            size--
            top -= SLOT_SIZE
            return
        }

        val leftSign = signOf(data, top)
        val rightSign = signOf(data, top - SLOT_SIZE)

        // special case 0 / any = 0, 0 % any = 0
        if (leftSign == 0 || rightSign == 0) {
            Arrays.fill(data, top - SLOT_SIZE, top, 0)
            size--
            top -= SLOT_SIZE
            return
        }

        // perform unsigned div mod of their complement
        if (leftSign < 0)
            complement(data, top)

        if (rightSign < 0)
            complement(data, top - SLOT_SIZE)

        // mark the sign of result
        val sign = if (rem) {
            leftSign
        } else {
            leftSign * rightSign
        }

        popOperand0()
        popOperand1()
        // divided as uint256
        divModInternal(rem)

        if (rem) {
            push(remMut)
        } else {
            push(varMut)
        }

        if (sign < 0) {
            complement(data, top)
        }
    }

    override fun not() {
        if (size == 0)
            throw RuntimeException("stack underflow")
        for (i in top until top + SLOT_SIZE) {
            data[i] = data[i].inv()
        }
    }

    override fun eq() {
        if (size < 2)
            throw RuntimeException("stack underflow")

        val cmp = compareTo(data, top, data, top - SLOT_SIZE, SLOT_SIZE)
        dropTwice()

        if (cmp == 0) {
            pushOne()
        } else {
            pushZero()
        }
    }

    override fun lt() {
        if (size < 2)
            throw RuntimeException("stack underflow")

        val cmp = compareTo(data, top, data, top - SLOT_SIZE, SLOT_SIZE)
        dropTwice()
        push(cmp < 0)
    }

    override fun slt() {
        if (size < 2)
            throw RuntimeException("stack underflow")

        val leftSign = signOf(data, top)
        val rightSign = signOf(data, top - SLOT_SIZE)

        if (leftSign != rightSign) {
            dropTwice()
            push(leftSign < rightSign)
            return
        }

        val cmp = compareTo(data, top, data, top - SLOT_SIZE, SLOT_SIZE) * leftSign * rightSign
        dropTwice()
        push(cmp < 0)
    }

    override fun gt() {
        if (size < 2)
            throw RuntimeException("stack underflow")

        val cmp = compareTo(data, top, data, top - SLOT_SIZE, SLOT_SIZE)
        dropTwice()
        push(cmp > 0)
    }

    override fun sgt() {
        if (size < 2)
            throw RuntimeException("stack underflow")

        val leftSign = signOf(data, top)
        val rightSign = signOf(data, top - SLOT_SIZE)

        if (leftSign != rightSign) {
            dropTwice()
            push(leftSign > rightSign)
            return
        }

        val cmp = compareTo(data, top, data, top - SLOT_SIZE, SLOT_SIZE) * leftSign * rightSign
        dropTwice()
        push(cmp > 0)
    }

    private fun dropNocheck() {
        size -= 1
        top -= SLOT_SIZE
    }

    override fun drop() {
        if (size < 1)
            throw RuntimeException("stack underflow")
        dropNocheck()
    }

    private fun dropTwice() {
        if (size < 2)
            throw RuntimeException("stack underflow")
        size -= 2
        top -= SLOT_SIZE * 2
    }

    override fun isZero() {
        if (size == 0)
            throw RuntimeException("stack underflow")

        var isZero = true
        for (i in top until top + SLOT_SIZE) {
            if (data[i] != 0) {
                isZero = false
                break
            }
        }
        drop()
        push(isZero)
    }

    override fun byte() {
        if (size < 2)
            throw RuntimeException("stack underflow")
        val i = popUnsignedInt()
        if (i < 0 || i >= SLOT_BYTE_ARRAY_SIZE) {
            drop()
            pushZero()
            return
        }
        encodeBE(data, top, tempBytes, 0)
        val j = tempBytes[i.toInt()].toUByte().toInt()
        drop()
        pushInt(j)
    }

    override fun shl() {
        if (size < 2)
            throw RuntimeException("stack underflow")
        val i = popUnsignedInt()
        if (i < 0 || i >= SLOT_BITS) {
            drop()
            pushZero()
            return
        }
        val c = i.toInt()
        leftShift(data, top, c / INT_BITS, c % INT_BITS)
    }

    override fun shr() {
        if (size < 2)
            throw RuntimeException("stack underflow")
        val i = popUnsignedInt()
        if (i < 0 || i >= SLOT_BITS) {
            drop()
            pushZero()
            return
        }
        val c = i.toInt()
        rightShift(data, top, c / INT_BITS, c % INT_BITS)
    }

    override fun sar() {
        if (size < 2)
            throw RuntimeException("stack underflow")
        val i = popUnsignedInt()
        if (i < 0 || i >= SLOT_BITS) {
            if (data[top] < 0) {
                drop()
                push(NEGATIVE_ONE)
            } else {
                drop()
                pushZero()
            }
            return
        }
        val c = i.toInt()
        signedRightShift(data, top, c / INT_BITS, c % INT_BITS)
    }


    override fun sha3(mem: Memory, digest: Digest) {
        mem.resize(memSizeByOffAndLen(backUnsignedInt(), backUnsignedInt(1)))
        val off = popIntExact()
        val len = popIntExact()
        digest.digest(mem.data, off, len, tempBytes, 0)
        pushZero()
        decodeBE(tempBytes, 0, data, top)
    }

    override fun callDataLoad(input: ByteArray) {
        val offInt = unsignedMin(popUnsignedInt(), input.size.toLong()).toInt()
        val len = Math.min(32, input.size - offInt)
        Arrays.fill(tempBytes, 0)
        System.arraycopy(input, offInt, tempBytes, SLOT_BYTE_ARRAY_SIZE - len, len)
        pushZero()
        decodeBE(tempBytes, 0, data, top)
    }

    private fun unsignedMin(x: Long, y: Long): Long {
        return if (x + Long.MIN_VALUE < y + Long.MIN_VALUE) {
            x
        } else {
            y
        }
    }

    override fun dataCopy(mem: Memory, data: ByteArray) {
        val memOff = popUnsignedInt()
        val len = popUnsignedInt()
        mem.resize(memSizeByOffAndLen(memOff, len))
        val dataOff = unsignedMin(popUnsignedInt(), data.size.toLong()).toInt()
        val lenInt = Math.min(len.toInt(), data.size - dataOff)
        mem.write(memOff.toInt(), data, dataOff, lenInt)
    }

    override fun mod() {
        popOperand0()
        popOperand1()
        if (operandMut1.isZero) {
            pushZero()
        } else {
            divModInternal(true)
            push(remMut)
        }
    }


    private val tempBytes: ByteArray = ByteArray(SLOT_BYTE_ARRAY_SIZE)

    companion object {
        const val INITIAL_CAP = 2
        val P_2_256 = BigInteger.valueOf(2).pow(256)
        val P_MAX = P_2_256 - BigInteger.ONE
        val P_SIGNED_MAX = BigInteger.valueOf(2).pow(255) - BigInteger.ONE
    }
}
