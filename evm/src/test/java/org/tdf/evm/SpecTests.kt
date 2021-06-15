package org.tdf.evm

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.math.BigInteger

@RunWith(JUnit4::class)
class SpecTests {
    @Test
    fun testSignExt() {
        val stack = StackImpl()

        TestUtil.testSpec("testdata/testcases_signext.json",
            fun (x: BigInteger, y: BigInteger): BigInteger {
                stack.push(x)
                stack.push(y)
                stack.signExtend()
                return stack.popBigInt()
            }
        )
    }
}