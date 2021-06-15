package org.tdf.evm

import org.tdf.common.util.MutableBigInteger
import org.tdf.common.util.SlotUtils.*
import java.math.BigInteger
import java.util.*

interface Stack {
    val size: Int

    fun push(n: IntArray, offset: Int)

    fun push(n: MutableBigInteger)

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

    fun dropTwice()

    /**
     * pop top into buffer
     */
    fun pop(buf: IntArray, offset: Int)

    /**
     * pop as biginteger
     */
    fun popBigInt(signed: Boolean = false): BigInteger

    /**
     * pop as unsigned integer, return -1 if overflowed
     */
    fun popUnsignedInt(): Long

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


    fun dup(index: Int)
    fun swap(index: Int)
}

class StackImpl : Stack {

    private var top: Int = -SLOT_SIZE

    override var size: Int = 0
        private set

    private var data: IntArray = IntArray(SLOT_SIZE * INITIAL_CAP)

    var cap: Int = INITIAL_CAP

    private val operand0 = IntArray(SLOT_SIZE * 2)
    private val operandMut0 = MutableBigInteger(operand0)

    private val operand1 = IntArray(SLOT_SIZE * 2)
    private val operandMut1 = MutableBigInteger(operand1)

    private val varSlot = IntArray(SLOT_SIZE * 2)
    private val varMut = MutableBigInteger(varSlot)

    private val divisor = IntArray(SLOT_SIZE * 2)

    private val remSlot = IntArray(SLOT_SIZE * 2)
    private val remMut = MutableBigInteger(remSlot)


    private fun tryGrow() {
        if (size != cap)
            return
        cap *= 2
        val tmp = IntArray(cap * SLOT_SIZE)
        System.arraycopy(data, 0, tmp, 0, data.size)
        this.data = tmp
    }

    override fun push(n: MutableBigInteger) {
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

    override fun popUnsignedInt(): Long{
        for(i in 0 until SLOT_MAX_INDEX) {
            if(data[top + i] != 0) {
                drop()
                return -1
            }
        }
        val r = data[top + SLOT_MAX_INDEX]
        drop()
        return Integer.toUnsignedLong(r)
    }

    override fun push(n: IntArray, offset: Int) {
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
        if (n.signum() >= 0) {
            pushInternal(n)
            return
        }
        pushInternal(n.negate())
        complement(data, top)
    }

    override fun pushOne() {
        tryGrow()
        size++
        top += SLOT_SIZE
        System.arraycopy(ONE, 0, data, top, SLOT_SIZE)
    }

    override fun pushZero() {
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
    }

    override fun mul() {
        popOperand0(0)
        popOperand1(0)
        if (operandMut0.isZero || operandMut1.isZero) {
            pushZero()
            return
        }
        varMut.clear()
        operandMut0.multiply(operandMut1, varMut)
        push(varMut)
    }

    override fun mulMod() {
        popOperand0(0)
        popOperand1(0)
        varMut.clear()
        if (!operandMut0.isZero && !operandMut1.isZero) {
            operandMut0.multiply(operandMut1, varMut)
        }

        operandMut0.copyValue(varMut)
        varMut.clear()
        popOperand1(0)
        divModInternal(true)
        push(remMut)
    }

    override fun exp() {
        val num = popBigInt()
        val exp = popBigInt()
        push(num.modPow(exp, P_2_256))
    }

    override fun div() {
        popOperand0(0)
        popOperand1(0)
        if (operandMut1.isZero) {
            pushZero()
            return
        }
        divModInternal(false)
        push(varMut)
    }

    override fun dup(index: Int) {
        tryGrow()
        if (index >= size)
            throw RuntimeException("stack underflow")
        size++
        top += SLOT_SIZE
        System.arraycopy(data, index * SLOT_SIZE, data, top, SLOT_SIZE)
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


    private fun addInternal(): Long {
        if (size < 2)
            throw RuntimeException("stack underflow")
        val carry = add(data, top - SLOT_SIZE, data, top, data, top - SLOT_SIZE)
        size--
        top -= SLOT_SIZE
        return carry
    }

    override fun sub() {
        if (size < 2)
            throw RuntimeException("stack underflow")
        subMut(data, top, data, top - SLOT_SIZE, data, top)
        System.arraycopy(data, top, data, top - SLOT_SIZE, SLOT_SIZE)
        size--
        top -= SLOT_SIZE
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

    private fun popOperand0(carry: Long) {
        Arrays.fill(operand0, 0)
        pop(operand0, SLOT_SIZE)
        operand0[SLOT_SIZE - 1] = carry.toInt()
        operandMut0.setValue(operand0, operand0.size)
        operandMut0.normalize()
    }

    private fun popOperand1(carry: Long) {
        Arrays.fill(operand1, 0)
        pop(operand1, SLOT_SIZE)
        operand1[SLOT_SIZE - 1] = carry.toInt()
        operandMut1.setValue(operand1, operand1.size)
        operandMut1.normalize()
    }

    override fun addMod() {
        val carry = addInternal()
        // pop into operand0
        popOperand0(carry)
        // pop into operand1
        popOperand1(0)
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
        if(size < 2)
            throw RuntimeException("stack underflow")

        val n = popUnsignedInt()
        if(n < 0 || n > MAX_BYTE_ARRAY_SIZE - 1)
            return

        val bytesLong = n + 1
        val bytes = bytesLong.toInt()

        encodeBE(data, top, tempBytes, 0)

        Arrays.fill(tempBytes, 0, MAX_BYTE_ARRAY_SIZE - bytes, 0)
        if (tempBytes[MAX_BYTE_ARRAY_SIZE - bytes] >= 0) {
            decodeBE(tempBytes, 0, data, top)
            return
        }

        Arrays.fill(tempBytes, 0, MAX_BYTE_ARRAY_SIZE - bytes, 0xff.toByte())
        decodeBE(tempBytes, 0, data, top)
    }

    override fun signedDiv() {
        signedDivMod(false)
    }

    private fun signedDivMod(rem: Boolean) {
        if (size < 2)
            throw RuntimeException("stack underflow")

        // special case
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

        if (leftSign == 0 || rightSign == 0) {
            Arrays.fill(data, top - SLOT_SIZE, top, 0)
            size--
            top -= SLOT_SIZE
            return
        }


        if (leftSign < 0)
            complement(data, top)

        if (rightSign < 0)
            complement(data, top - SLOT_SIZE)

        val sign = if (rem) {
            leftSign
        } else {
            leftSign * rightSign
        }

        popOperand0(0)
        popOperand1(0)


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

        if (cmp < 0) {
            pushOne()
        } else {
            pushZero()
        }
    }

    override fun slt() {
        if (size < 2)
            throw RuntimeException("stack underflow")

        val leftSign = signOf(data, top)
        val rightSign = signOf(data, top - SLOT_SIZE)

        if (leftSign != rightSign) {
            dropTwice()
            if (leftSign < rightSign) {
                pushOne()
            } else {
                pushZero()
            }
            return
        }

        val cmp = compareTo(data, top, data, top - SLOT_SIZE, SLOT_SIZE) * leftSign * rightSign
        dropTwice()
        if (cmp < 0) {
            pushOne()
        } else {
            pushZero()
        }
    }

    override fun gt() {
        if (size < 2)
            throw RuntimeException("stack underflow")

        val cmp = compareTo(data, top, data, top - SLOT_SIZE, SLOT_SIZE)
        dropTwice()

        if (cmp > 0) {
            pushOne()
        } else {
            pushZero()
        }
    }

    override fun sgt() {
        if (size < 2)
            throw RuntimeException("stack underflow")

        val leftSign = signOf(data, top)
        val rightSign = signOf(data, top - SLOT_SIZE)

        if (leftSign != rightSign) {
            dropTwice()
            if (leftSign > rightSign) {
                pushOne()
            } else {
                pushZero()
            }
            return
        }

        val cmp = compareTo(data, top, data, top - SLOT_SIZE, SLOT_SIZE) * leftSign * rightSign
        dropTwice()
        if (cmp > 0) {
            pushOne()
        } else {
            pushZero()
        }
    }

    override fun drop() {
        if (size < 1)
            throw RuntimeException("stack underflow")
        size -= 1
        top -= SLOT_SIZE
    }

    override fun dropTwice() {
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
        if (isZero) {
            pushOne()
        } else {
            pushZero()
        }
    }

    override fun mod() {
        popOperand0(0)
        popOperand1(0)
        if (operandMut1.isZero) {
            pushZero()
        } else {
            divModInternal(true)
            push(remMut)
        }
    }


    private val tempBytes: ByteArray = ByteArray(MAX_BYTE_ARRAY_SIZE)

    companion object {
        const val INITIAL_CAP = 2
        val P_2_256 = BigInteger.valueOf(2).pow(256)
        val P_MAX = P_2_256 - BigInteger.ONE
    }
}
