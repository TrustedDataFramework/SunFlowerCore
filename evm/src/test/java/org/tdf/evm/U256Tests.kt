package org.tdf.evm


import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.tdf.common.util.SlotUtils.*
import java.math.BigInteger
import java.security.SecureRandom
import java.util.*

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
        for (i in 0 until 1000) {
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
        for (i in 0 until 1000) {
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
        for (i in 0 until 1000) {
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
        for (i in 0 until 1000) {
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
        for (i in 0 until 1000) {
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
        for (i in 0 until 1000) {
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

        for (j in 4..32) {
            val left = ByteArray(j)
            for (k in 4..32) {
                val right = ByteArray(k)
                for (i in 0 until 1000) {
                    SR.nextBytes(left)
                    SR.nextBytes(right)

                    val l = BigInteger(1, left)
                    val r = BigInteger(1, right)

                    if (r == BigInteger.ZERO)
                        continue

                    val expected = l / r
                    val actual = reg.div(l, r)

                    assertEquals(expected.toString(16), actual.toString(16))
                }
            }

        }

    }


    @Test
    fun testRandomMod() {
        val reg = U256Register()

        for (j in 4..32) {
            val left = ByteArray(j)
            for (k in 4..32) {
                val right = ByteArray(k)
                for (i in 0 until 1000) {
                    SR.nextBytes(left)
                    SR.nextBytes(right)

                    if(j >= 24 && k >= 24 && i == 3) {
                        for(x in 4 until j)
                            left[x] = 0
                        for(x in 4 until k)
                            right[x] = 0
                    }

                    var l = if (i == 0) {
                        BigInteger.ZERO
                    } else {
                        BigInteger(1, left)
                    }
                    var r = if (i == 1) {
                        l
                    } else {
                        BigInteger(1, right)
                    }

                    if(k == 4 && i == 2) {
                        l = r + r
                    }



                    if (r == BigInteger.ZERO)
                        continue



                    val expected = l % r
                    val actual = reg.mod(l, r)

                    if (actual != expected) {
                        println("l = $l r = $r")
                    }
                    assertEquals(expected.toString(16), actual.toString(16))
                }
            }

        }

    }

    @Test
    fun testFailed() {
        val lefts = longArrayOf(252584348, 1811528554, 3654679626)
        val rights = longArrayOf(3676406621, 2111400643, 1969366594)

        for (i in 0 until lefts.size) {
            val l = BigInteger.valueOf(lefts[i])
            val r = BigInteger.valueOf(rights[i])
            val reg = U256Register()
            val actual = reg.mod(l, r)
            assertEquals(l % r, actual)
        }
    }
}