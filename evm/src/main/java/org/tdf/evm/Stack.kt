package org.tdf.evm;

import org.tdf.common.util.MutableBigInteger
import org.tdf.common.util.SlotUtils
import org.tdf.common.util.SlotUtils.MAX_BYTE_ARRAY_SIZE
import org.tdf.common.util.SlotUtils.SLOT_SIZE
import java.math.BigInteger

interface Stack {
    fun push(n: IntArray, offset: Int)

    fun push(n: MutableBigInteger)

    fun pushZero()

    /**
     * push a value to stack
     */
    fun push(n: BigInteger)

    /**
     * push a value to stack
     */
    fun pushLong(n: Long)

    /**
     * push a value to stack
     */
    fun pushInt(n: Int)

    /**
     * pop top into slot
     */
    fun pop(buf: IntArray, offset: Int)

    fun popBigInt(): BigInteger

    // arithmetic, unsigned
    fun add()
    fun sub()
    fun mul()
    fun div()
    fun mod()
    fun addMod()

    // arithmetic, signed
    fun signedDiv()

    fun not()
    fun eq()
    fun isZero()


    fun dup(index: Int)
    fun swap(index: Int)
}

class StackImpl : Stack {

    private var top: Int = -SLOT_SIZE

    var size: Int = 0
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
        System.arraycopy(ZERO_SLOT, 0, data, top + SLOT_SIZE, SLOT_SIZE)
        SlotUtils.trim256(n.value, n.offset, n.intLen, data, top + SLOT_SIZE)
        size++
        top += SLOT_SIZE
    }

    override fun pop(buf: IntArray, offset: Int) {
        if (size == 0)
            throw RuntimeException("stack underflow")
        System.arraycopy(data, top, buf, offset, SLOT_SIZE)
        size--
        top -= SLOT_SIZE
    }

    override fun popBigInt(): BigInteger {
        if (size == 0)
            throw RuntimeException("stack underflow")
        SlotUtils.encodeBE(data, top, tempBytes, 0)
        val r = BigInteger(1, tempBytes)
        size--
        top -= SLOT_SIZE
        return r
    }

    override fun push(n: IntArray, offset: Int) {
        tryGrow()
        System.arraycopy(n, offset, data, top + SLOT_SIZE, SLOT_SIZE)
        size++
        top += SLOT_SIZE
    }

    override fun push(n: BigInteger) {
        tryGrow()
        resetTempBytes()
        SlotUtils.copyFrom(tempBytes, 0, n)
        SlotUtils.decodeBE(tempBytes, 0, data, top + SLOT_SIZE)
        size++
        top += SLOT_SIZE
    }

    override fun pushZero() {
        tryGrow()
        System.arraycopy(ZERO_SLOT, 0, data, top + SLOT_SIZE, SLOT_SIZE)
        size++
        top += SLOT_SIZE
    }

    override fun pushLong(n: Long) {
        tryGrow()
        // clear top slot
        System.arraycopy(ZERO_SLOT, 0, data, top + SLOT_SIZE, 2)
        data[top + SLOT_SIZE + 2] = (n ushr 32).toInt()
        data[top + SLOT_SIZE + 3] = n.toInt()
        size++
        top += SLOT_SIZE
    }

    override fun pushInt(n: Int) {
        tryGrow()
        // clear top slot
        System.arraycopy(ZERO_SLOT, 0, data, top + SLOT_SIZE, 3)
        data[top + SLOT_SIZE + 3] = n
        size++
        top += SLOT_SIZE
    }

    override fun add() {
        addInternal()
    }

    override fun mul() {
        popOperand0(0)
        popOperand1(0)
        if(operandMut0.isZero || operandMut1.isZero) {
            pushZero()
            return
        }
        varMut.clear()
        operandMut0.multiply(operandMut1, varMut)
        push(varMut)
    }

    override fun div() {
        popOperand0(0)
        popOperand1(0)
        if(operandMut1.isZero) {
            pushZero()
            return
        }
        divModInternal(false)
        push(varMut)
    }

    override fun dup(index: Int) {
        tryGrow()
        if(index >= size)
            throw RuntimeException("stack underflow")
        System.arraycopy(data, index * SLOT_SIZE, data, top + SLOT_SIZE, SLOT_SIZE)
        size++
        top += SLOT_SIZE
    }

    override fun swap(index: Int) {
        if(index >= size)
            throw RuntimeException("stack underflow")
        if(index == size - 1)
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
        val carry = SlotUtils.add(data, top - SLOT_SIZE, data, top, data, top - SLOT_SIZE)
        size--
        top -= SLOT_SIZE
        return carry
    }

    override fun sub() {
        if (size < 2)
            throw RuntimeException("stack underflow")
        SlotUtils.subMut(data, top, data, top - SLOT_SIZE, data, top)
        System.arraycopy(data, top, data, top - SLOT_SIZE, SLOT_SIZE);
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
        System.arraycopy(ZERO_SLOT, 0, divisor, 0, SLOT_SIZE * 2)
        // clear rem
        remMut.clear()
        operandMut0.divideKnuth(operandMut1, varMut, remMut, divisor, needRem)
    }

    private fun popOperand0(carry: Long) {
        System.arraycopy(ZERO_SLOT, 0, operand0, 0, SLOT_SIZE)
        pop(operand0, SLOT_SIZE)
        operand0[SLOT_SIZE - 1] = carry.toInt()
        operandMut0.setValue(operand0, operand0.size)
        operandMut0.normalize()
    }

    private fun popOperand1(carry: Long) {
        System.arraycopy(ZERO_SLOT, 0, operand1, 0, SLOT_SIZE)
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
        if(operandMut1.isZero) {
            pushZero()
            return
        }
        divModInternal(true)
        push(remMut)
    }

    override fun signedDiv() {
        popOperand0(0)
        popOperand1(0)

        if(operandMut1.isZero) {
            pushZero()
            return
        }

        val sign = SlotUtils.sign(operandMut0) * SlotUtils.sign(operandMut1)
        divModInternal(false)

        push(varMut)

        if(sign < 0) {
            not()
            pushInt(1)
            add()
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

        val cmp = SlotUtils.compareTo(data, top, data, top - SLOT_SIZE, SLOT_SIZE)
        popInternal()
        popInternal()
        pushInt(
            if (cmp == 0) {
                1
            } else {
                0
            }
        )
    }


    private fun popInternal() {
        size -= 1
        top -= SLOT_SIZE
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

        popInternal()
        pushInt(
            if (isZero) {
                1
            } else {
                0
            }
        )
    }

    override fun mod() {
        popOperand0(0)
        popOperand1(0)
        if(operandMut1.isZero) {
            pushZero()
        } else {
            divModInternal(true)
            push(remMut)
        }
    }


    protected val tempBytes: ByteArray = ByteArray(MAX_BYTE_ARRAY_SIZE)

    protected fun resetTempBytes() {
        System.arraycopy(ZERO_BYTES, 0, tempBytes, 0, MAX_BYTE_ARRAY_SIZE)
    }

    protected fun resetSlot(slot: IntArray, offset: Int, size: Int) {
        System.arraycopy(ZERO_SLOT, 0, slot, offset, size)
    }

    companion object {
        val ZERO_BYTES: ByteArray = ByteArray(MAX_BYTE_ARRAY_SIZE)
        val ZERO_SLOT: IntArray = IntArray(SLOT_SIZE * 2)
        val ONE: IntArray = intArrayOf(0, 0, 0, 0, 0, 0, 0, 1)
        const val INITIAL_CAP = 2
    }

}
