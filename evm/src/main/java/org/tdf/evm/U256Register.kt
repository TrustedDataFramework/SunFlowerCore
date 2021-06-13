package org.tdf.evm;

import org.tdf.common.util.MutableBigInteger
import org.tdf.common.util.SlotUtils
import org.tdf.common.util.SlotUtils.SLOT_SIZE
import java.math.BigInteger

class U256Register {
    companion object {
        const val MAX_POW = 256
        val _2_256 = BigInteger.valueOf(2).pow(MAX_POW)
    }

    // slot0 and slot1 is used for store operand
    private val slot0 = IntArray(SLOT_SIZE)
    private val slot1 = IntArray(SLOT_SIZE)

    // variable length slot, used for multiply
    private val slot2 = IntArray(SLOT_SIZE * 2)

    private val slot3 = IntArray(SLOT_SIZE)
    private val slot4 = IntArray(SLOT_SIZE)
    private val slot5 = IntArray(SLOT_SIZE)

    fun add(left: BigInteger, right: BigInteger): BigInteger {
        slot0.copyFrom(left)
        slot1.copyFrom(right)
        slot0.add(slot1)
        return slot0.toBigInt()
    }

    fun sub(left: BigInteger, right: BigInteger): BigInteger {
        slot0.copyFrom(left)
        slot1.copyFrom(right)
        slot0.sub(slot1)
        return slot0.toBigInt()
    }

    fun mul(left: BigInteger, right: BigInteger): BigInteger {
        slot0.copyFrom(left)
        slot1.copyFrom(right)
        slot2.reset()

        val m0 = MutableBigInteger(slot0)
        m0.normalize()

        val m1 = MutableBigInteger(slot1)
        m1.normalize()

        val m2 = MutableBigInteger(slot2)
        m0.multiply(m1, m2)

        return m2.toBigInt().mod(_2_256)
    }

    fun div(left: BigInteger, right: BigInteger): BigInteger {
        slot0.copyFrom(left)
        val m = MutableBigInteger(slot0)
        m.normalize()

        slot1.reset()
        slot1.copyFrom(right)
        val m1 = MutableBigInteger(slot1)
        m1.normalize()


        slot2.reset()
        val quo = MutableBigInteger(slot2)

        m.divide(m1, quo, false)
        return quo.toBigInt()
    }

}