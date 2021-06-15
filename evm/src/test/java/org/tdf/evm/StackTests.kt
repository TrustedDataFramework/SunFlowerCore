package org.tdf.evm

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.math.BigInteger

interface TestOperator {
    fun skip(left: BigInteger, right: BigInteger): Boolean
    fun expect(left: BigInteger, right: BigInteger): BigInteger
    fun actual(left: BigInteger, right: BigInteger): BigInteger
}

object StackAdd: TestOperator {
    private val stack: Stack = StackImpl()

    override fun skip(left: BigInteger, right: BigInteger): Boolean {
        return false
    }

    override fun expect(left: BigInteger, right: BigInteger): BigInteger {
        return (left + right) % U256Tests._2_256
    }

    override fun actual(left: BigInteger, right: BigInteger): BigInteger {
        stack.push(left)
        stack.push(right)
        stack.add()
        return stack.popBigInt()
    }

}

object StackMul: TestOperator {
    private val stack: Stack = StackImpl()

    override fun skip(left: BigInteger, right: BigInteger): Boolean {
        return false
    }

    override fun expect(left: BigInteger, right: BigInteger): BigInteger {
        return (left * right) % U256Tests._2_256
    }

    override fun actual(left: BigInteger, right: BigInteger): BigInteger {
        stack.push(left)
        stack.push(right)
        stack.mul()
        return stack.popBigInt()
    }
}

object StackDiv: TestOperator {
    private val stack: Stack = StackImpl()

    override fun skip(left: BigInteger, right: BigInteger): Boolean {
        return false
    }

    override fun expect(left: BigInteger, right: BigInteger): BigInteger {
        return if(right == BigInteger.ZERO) { BigInteger.ZERO } else { left / right }
    }

    override fun actual(left: BigInteger, right: BigInteger): BigInteger {
        stack.push(right)
        stack.push(left)
        stack.div()
        return stack.popBigInt()
    }
}

object StackMod: TestOperator {
    private val stack: Stack = StackImpl()

    override fun skip(left: BigInteger, right: BigInteger): Boolean {
        return false
    }

    override fun expect(left: BigInteger, right: BigInteger): BigInteger {
        return if(right == BigInteger.ZERO) { BigInteger.ZERO } else { left % right }
    }

    override fun actual(left: BigInteger, right: BigInteger): BigInteger {
        stack.push(right)
        stack.push(left)
        stack.mod()
        return stack.popBigInt()
    }
}


@RunWith(JUnit4::class)
class StackTests {
    @Test
    fun testRandomAdd() {
        TestUtil.arithmeticTest(StackAdd)
    }

    @Test
    fun testRandomMul() {
        TestUtil.arithmeticTest(StackMul)
    }

    @Test
    fun testRandomDiv() {
        TestUtil.arithmeticTest(StackDiv)
    }

    @Test
    fun testRandomMod() {
        TestUtil.arithmeticTest(StackMod)
    }

    @Test
    fun testFailed() {
        TestUtil.testSinglePair(StackDiv, BigInteger.valueOf(3510866428), BigInteger.valueOf(1755433214))
    }
}