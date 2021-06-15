package org.tdf.evm

import org.junit.Assert
import java.math.BigInteger

object TestUtil {
    fun testSinglePair(op: TestOperator, l: BigInteger, r: BigInteger) {
        if(op.skip(l, r))
            return
        val expected = op.expect(l, r)
        val actual = op.actual(l, r)

        if (actual != expected) {
            println("test failed: l = $l r = $r")
        }
        Assert.assertEquals("", expected.toString(16), actual.toString(16))
    }

    fun arithmeticTest(op: TestOperator) {
        for (j in 0..32) {
            val left = ByteArray(j)
            for (k in 0..32) {
                val right = ByteArray(k)
                for (i in 0 until 1000) {
                    U256Tests.SR.nextBytes(left)
                    U256Tests.SR.nextBytes(right)

                    if (j >= 24 && k >= 24 && i == 3) {
                        for (x in 4 until j)
                            left[x] = 0
                        for (x in 4 until k)
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

                    if (k == 4 && i == 2) {
                        l = r + r
                    }

                    testSinglePair(op, l, r)
                }
            }

        }

    }

}