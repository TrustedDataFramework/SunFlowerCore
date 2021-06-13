package org.tdf.evm;

import java.math.BigInteger

interface Stack256 {
    fun push(n: IntArray, offset: Int)

    /**
     * push a value to stack
     */
    fun push(n: BigInteger)

    /**
     * push a value to stack
     */
    fun push(n: Long)

    /**
     * push a value to stack
     */
    fun push(n: Int)

    /**
     * pop as big integer
     */
    fun popBigInt(): BigInteger
    fun popInto(data: IntArray)
    fun popInto(data: ByteArray)

    fun add()
    fun sub()
}

abstract class AbstractStack256: Stack256 {
    override fun popBigInt(): BigInteger {
        val bytes = ByteArray(32)
        popInto(bytes)
        return BigInteger(1, bytes)
    }

}