package org.tdf.evm


import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.math.BigInteger
import java.security.SecureRandom
import java.util.*
import org.junit.Assert.*;
import org.tdf.common.util.MutableBigInteger
import org.tdf.common.util.SlotUtils.MAX_BYTE_ARRAY_SIZE
import org.tdf.common.util.SlotUtils.SLOT_SIZE

@RunWith(JUnit4::class)
class U256Tests {
    companion object {
        const val MAX_POW = 256
        val _2_256 = BigInteger.valueOf(2).pow(MAX_POW)
        val MAX_U256 = BigInteger("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16)
        val SR = SecureRandom()
    }

    @Test
    fun testLoadSlot() {
        for(i in 0 until 1000) {
            val left = ByteArray(MAX_BYTE_ARRAY_SIZE)
            SR.nextBytes(left)

            val slot = IntArray(SLOT_SIZE)
            slot.copyFrom(left)
            val actual = slot.toByteArray()
            assertArrayEquals(left, actual)
        }
    }

    @Test
    fun testLoadSlot1() {
        for(i in 0 until 1000) {
            val left = ByteArray(16)
            SR.nextBytes(left)

            val slot = IntArray(SLOT_SIZE)
            slot.copyFrom(left)
            val actual = Arrays.copyOfRange(
                slot.toByteArray(),
                MAX_BYTE_ARRAY_SIZE - 16,
                MAX_BYTE_ARRAY_SIZE
            )
            assertArrayEquals(left, actual)
        }
    }


    @Test
    fun testLoadSlot2() {
        for(i in 0 until 1000) {
            val left = ByteArray(16)
            SR.nextBytes(left)
            val l = BigInteger(1, left)
            val slot = IntArray(SLOT_SIZE)

            slot.copyFrom(left)
            val r = slot.toBigInt()

            assertEquals(l, r)
        }
    }


    @Test
    fun testAdd() {
        val reg = U256Register()

            val l = BigInteger.valueOf(Long.MAX_VALUE)
            val r = BigInteger.valueOf(Long.MAX_VALUE)

            val expected = l + r

            val actual = reg.add(l, r).mod(_2_256)

            assertEquals(expected.toString(16), actual.toString(16))

    }

    @Test
    fun testRandomAdd() {
        val reg = U256Register()
        for(i in 0 until 1000) {
            val left = ByteArray(MAX_BYTE_ARRAY_SIZE)
            SR.nextBytes(left)

            val right = ByteArray(MAX_BYTE_ARRAY_SIZE)
            SR.nextBytes(right)

            val l = BigInteger(1, left)
            val r = BigInteger(1, right)

            val expected = (l + r).mod(_2_256)

            val actual = reg.add(l, r)

            assertEquals(expected.toString(16), actual.toString(16))
        }
    }

    @Test
    fun testRandomSub() {
        val reg = U256Register()
        for(i in 0 until 1000) {
            val left = ByteArray(MAX_BYTE_ARRAY_SIZE)
            SR.nextBytes(left)

            val right = ByteArray(MAX_BYTE_ARRAY_SIZE)
            SR.nextBytes(right)

            val l = BigInteger(1, left)
            val r = BigInteger(1, right)

            val expected = l.subtract(r).mod(_2_256)
            val actual = reg.sub(l, r)

            assertEquals(expected.toString(16), actual.toString(16))
        }
    }

    @Test
    fun testMul() {
        val reg = U256Register()
            val l = BigInteger.valueOf(Long.MAX_VALUE)
            val r = BigInteger.valueOf(Long.MAX_VALUE)

            val expected = (l * r).mod(_2_256)
            val actual = reg.mul(l, r)

            assertEquals(expected.toString(16), actual.toString(16))

    }

    @Test
    fun testRandomMul() {
        val reg = U256Register()
        val left = ByteArray(MAX_BYTE_ARRAY_SIZE)
        val right = ByteArray(MAX_BYTE_ARRAY_SIZE)
        for(i in 0 until 1000) {
            SR.nextBytes(left)
            SR.nextBytes(right)

            val l = BigInteger(1, left)
            val r = BigInteger(1, right)

            val expected = (l * r).mod(_2_256)
            val actual = reg.mul(l, r)

            assertEquals(expected.toString(16), actual.toString(16))
        }
    }


    @Test
    fun test2() {
        val a = BigInteger("FFFFFFFFFFFFFFFF", 16)
        val slot = IntArray(SLOT_SIZE)
        slot.copyFrom(a.toByteArray())
        println(Arrays.toString(slot))
        val r = U256Register()
        println(Arrays.toString(slot))
    }

    @Test
    fun testRandomDiv() {
        val reg = U256Register()

        for(j in 4..32) {
            val left = ByteArray(j)
            for(k in 4..32) {
                val right = ByteArray(k)
                for(i in 0 until 1000) {
                    SR.nextBytes(left)
                    SR.nextBytes(right)

                    val l = BigInteger(1, left)
                    val r = BigInteger(1, right)

                    if(r == BigInteger.ZERO)
                        continue

                    val expected = l / r
                    val actual = reg.div(l, r)

                    assertEquals(expected.toString(16), actual.toString(16))
                }
            }

        }

    }

    @Test
    fun testMutableBigIntegers() {
        val x = MutableBigInteger(intArrayOf(0x7d, 0x1a8d1d4f))
        val y = MutableBigInteger(intArrayOf((0xec8688abu).toInt()))
        val z = MutableBigInteger()

        x.divide(y, z)
        println(z)
    }



//    @Test
//    fun testRandomMod() {
//        val reg = U256Register()
//        for(i in 0 until 1000) {
//            val left = ByteArray(32)
//            SR.nextBytes(left)
//
//            val right = ByteArray(32)
//            SR.nextBytes(right)
//
//            val l = BigInteger(1, left)
//            val r = BigInteger(1, right)
//
//            if(r == BigInteger.ZERO)
//                continue
//
//            val expected = l % r
//            val actual = reg.mod(l, r)
//
//            assertEquals(expected.toString(16), actual.toString(16))
//        }
//    }

    @Test
    fun testFailed() {
        val left = "e963035f3c6b92ba25983f75dce5cd07db9cf5f473c0790db5efcd32281e3660"
        val right = "9abbc50c7c6bd0ba8974f70aec50e5adfbd03224b54c887bd7c46f7103699c65"
        val l = BigInteger(left, 16)
        val r = BigInteger(right, 16)
        val reg = U256Register()
        reg.add(l, r)
    }
}