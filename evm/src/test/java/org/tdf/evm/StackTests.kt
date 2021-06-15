package org.tdf.evm

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.tdf.evm.StackImpl.Companion.P_2_256
import org.tdf.evm.StackImpl.Companion.P_MAX
import java.math.BigInteger

class StackTestOperator(
    private val expect: (BigInteger, BigInteger) -> BigInteger,
    private val actual: (Stack) -> BigInteger,
    private val skip: (BigInteger, BigInteger) -> Boolean = { _, _ -> false }
) : TestBinaryOperator {
    private val stack = StackImpl()

    override fun skip(left: BigInteger, right: BigInteger): Boolean {
        return skip.invoke(left, right)
    }

    override fun expect(left: BigInteger, right: BigInteger): BigInteger {
        return expect.invoke(left, right)
    }

    override fun actual(left: BigInteger, right: BigInteger): BigInteger {
        stack.push(right)
        stack.push(left)
        return actual.invoke(stack)
    }
}


val stackAdd = StackTestOperator(
    { l, r -> (l + r) % P_2_256 },
    fun(stack: Stack): BigInteger {
        stack.add()
        return stack.popBigInt()
    }
)

val stackSub = StackTestOperator(
    { l, r -> (l - r).and(P_MAX) },
    fun(stack: Stack): BigInteger {
        stack.sub()
        return stack.popBigInt()
    }
)

val stackMul = StackTestOperator(
    { l, r -> (l * r) % P_2_256 },
    fun(stack: Stack): BigInteger {
        stack.mul()
        return stack.popBigInt()
    }
)

val stackDiv = StackTestOperator(
    { l, r ->
        if (r == BigInteger.ZERO) {
            BigInteger.ZERO
        } else {
            l / r
        }
    },
    fun(stack: Stack): BigInteger {
        stack.div()
        return stack.popBigInt()
    }
)

val stackMod = StackTestOperator(
    { l, r ->
        if (r == BigInteger.ZERO) {
            BigInteger.ZERO
        } else {
            l % r
        }
    },
    fun(stack: Stack): BigInteger {
        stack.mod()
        return stack.popBigInt()
    }
)

val stackSDiv = StackTestOperator(
    { l, r ->
        if (r == BigInteger.ZERO) {
            BigInteger.ZERO
        } else {
            l / r
        }
    },
    fun(stack: Stack): BigInteger {
        stack.signedDiv()
        return stack.popBigInt(true)
    }
)

val stackSMod = StackTestOperator(
    { l, r ->
        if (r == BigInteger.ZERO) {
            BigInteger.ZERO
        } else {
            l % r
        }
    },
    fun(stack: Stack): BigInteger {
        stack.signedMod()
        return stack.popBigInt(true)
    }
)

val stackLt = StackTestOperator(
    { l, r ->
        if (l < r) {
            BigInteger.ONE
        } else {
            BigInteger.ZERO
        }
    },
    fun(stack: Stack): BigInteger {
        stack.lt()
        return stack.popBigInt()
    }
)

val stackSLt = StackTestOperator(
    { l, r ->
        if (l < r) {
            BigInteger.ONE
        } else {
            BigInteger.ZERO
        }
    },
    fun(stack: Stack): BigInteger {
        stack.slt()
        return stack.popBigInt()
    }
)

val stackGt = StackTestOperator(
    { l, r ->
        if (l > r) {
            BigInteger.ONE
        } else {
            BigInteger.ZERO
        }
    },
    fun(stack: Stack): BigInteger {
        stack.gt()
        return stack.popBigInt()
    }
)

val stackSGt = StackTestOperator(
    { l, r ->
        if (l > r) {
            BigInteger.ONE
        } else {
            BigInteger.ZERO
        }
    },
    fun(stack: Stack): BigInteger {
        stack.sgt()
        return stack.popBigInt()
    }
)

val stackAnd = StackTestOperator(
    { l, r ->
        l.and(r)
    },
    fun(stack: Stack): BigInteger {
        stack.and()
        return stack.popBigInt()
    }
)

val stackOr = StackTestOperator(
    { l, r ->
        l.or(r)
    },
    fun(stack: Stack): BigInteger {
        stack.or()
        return stack.popBigInt()
    }
)

val stackXor = StackTestOperator(
    { l, r ->
        l.xor(r)
    },
    fun(stack: Stack): BigInteger {
        stack.xor()
        return stack.popBigInt()
    }
)

val stackEq = StackTestOperator(
    { l, r ->
        if (l == r) {
            BigInteger.ONE
        } else {
            BigInteger.ZERO
        }
    },
    fun(stack: Stack): BigInteger {
        stack.eq()
        return stack.popBigInt()
    }
)

val stackIsZero = StackTestOperator(
    { l, _ ->
        if (l == BigInteger.ZERO) {
            BigInteger.ONE
        } else {
            BigInteger.ZERO
        }
    },
    fun(stack: Stack): BigInteger {
        stack.isZero()
        return stack.popBigInt()
    }
)

val stackExp = StackTestOperator(
    { l, r ->
        l.modPow(r, P_2_256)
    },
    fun(stack: Stack): BigInteger {
        stack.exp()
        return stack.popBigInt()
    }
)

val stackDup = StackTestOperator(
    { _, r -> r },
    fun(stack: Stack): BigInteger {
        stack.dup(stack.size - 2)
        return stack.popBigInt()
    }
)

@RunWith(JUnit4::class)
class StackTests {
    @Test
    fun testRandomAdd() {
        TestUtil.unsignedArithmeticTest(stackAdd)
    }

    @Test
    fun testRandomSub() {
        TestUtil.unsignedArithmeticTest(stackSub)
    }

    @Test
    fun testRandomMul() {
        TestUtil.unsignedArithmeticTest(stackMul)
    }

    @Test
    fun testRandomDiv() {
        TestUtil.unsignedArithmeticTest(stackDiv)
    }

    @Test
    fun testRandomMod() {
        TestUtil.unsignedArithmeticTest(stackMod)
    }

    @Test
    fun testRandomSignedDiv() {
        TestUtil.signedArithmeticTest(stackSDiv)
    }


    @Test
    fun testRandomSignedMod() {
        TestUtil.signedArithmeticTest(stackSMod)
    }

    @Test
    fun testRandomLt() {
        TestUtil.unsignedArithmeticTest(stackLt)
    }


    @Test
    fun testRandomSLt() {
        TestUtil.signedArithmeticTest(stackSLt)
    }

    @Test
    fun testRandomGt() {
        TestUtil.unsignedArithmeticTest(stackGt)
    }


    @Test
    fun testRandomSGt() {
        TestUtil.signedArithmeticTest(stackSGt)
    }

    @Test
    fun testRandomAnd() {
        TestUtil.unsignedArithmeticTest(stackAnd)
    }

    @Test
    fun testRandomOr() {
        TestUtil.unsignedArithmeticTest(stackOr)
    }

    @Test
    fun testRandomXor() {
        TestUtil.unsignedArithmeticTest(stackXor)
    }

    @Test
    fun testRandomEq() {
        TestUtil.unsignedArithmeticTest(stackEq)
    }

    @Test
    fun testRandomIsZero() {
        TestUtil.unsignedArithmeticTest(stackIsZero)
    }

    @Test
    fun testRandomExp() {
        TestUtil.unsignedArithmeticTest(stackExp, 2)
    }

    @Test
    fun testRandomDup() {
        TestUtil.unsignedArithmeticTest(stackDup, 2)
    }

    @Test
    fun testFailed() {
        val l = BigInteger.valueOf(0)
        val r = BigInteger.valueOf(0)
        TestUtil.testSinglePair(stackSub, l, r)
    }
}