package org.tdf.evm

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.Assert
import org.spongycastle.util.encoders.Hex
import org.tdf.common.util.SlotUtils
import org.tdf.common.util.SlotUtils.SLOT_SIZE
import java.io.InputStream
import java.math.BigInteger
import kotlin.experimental.and

interface TestBinaryOperator {
    fun skip(left: BigInteger, right: BigInteger): Boolean
    fun expect(left: BigInteger, right: BigInteger): BigInteger
    fun actual(left: BigInteger, right: BigInteger): BigInteger
}



data class EvmSpec(
    @JsonProperty("X") val x: String = "",
    @JsonProperty("Y") val y: String = "",
    @JsonProperty("Expected") val expected: String = ""
) {
    val xData: IntArray
    val yData: IntArray

    init {
        xData = IntArray(SLOT_SIZE)
        yData = IntArray(SLOT_SIZE)
        val xBytes = Hex.decode(x)
        val yBytes = Hex.decode(y)
        SlotUtils.decodeBE(xBytes, 0, xData, 0)
        SlotUtils.decodeBE(yBytes, 0, yData, 0)
    }
}

object TestUtil {
    private val objectMapper: ObjectMapper = jacksonObjectMapper()


    fun testSpec(fileName: String, op: (EvmSpec) -> String) {
        val bytes = readClassPathFile(fileName)
        val cases = objectMapper.readValue(bytes, Array<EvmSpec>::class.java)

        for (c in cases) {
            val actual: String
            try {
                actual = op.invoke(c)
            } catch (e: Exception) {
                println("exception!! case = $c")
                throw e
            }
            if (c.expected != actual) {
                println("case = $c")
                throw RuntimeException("expected ${c.expected} while $actual returned")
            }
        }
    }

    fun readClassPathFile(name: String): ByteArray {
        val stream: InputStream = TestUtil::class.java.getClassLoader().getResource(name)!!.openStream()
        val all = ByteArray(stream.available())
        if (stream.read(all) != all.size) throw RuntimeException("read bytes from stream failed")
        return all
    }

    private const val LOOPS = 100

    fun testSinglePair(op: TestBinaryOperator, l: BigInteger, r: BigInteger) {
        if (op.skip(l, r))
            return
        val expected = op.expect(l, r)
        val actual = op.actual(l, r)

        if (actual != expected) {
            println("test failed: l = $l r = $r")
        }
        Assert.assertEquals("", expected.toString(16), actual.toString(16))
    }

    fun unsignedArithmeticTest(op: TestBinaryOperator, loops: Int = LOOPS) {
        for (j in 0..32) {
            val left = ByteArray(j)
            for (k in 0..32) {
                val right = ByteArray(k)
                for (i in 0 until loops) {
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
                    val r = if (i == 1) {
                        l
                    } else {
                        BigInteger(1, right)
                    }

                    if (k == 4 && i == 2) {
                        l = r + r
                    }

                    try {
                        testSinglePair(op, l, r)
                    } catch (e: Exception) {
                        println("exception found l = $l r = $r, op = $op")
                        throw e
                    }
                }
            }
        }
    }

    fun signedArithmeticTest(op: TestBinaryOperator, loops: Int = LOOPS) {
        for (j in 0..32) {
            val left = ByteArray(j)
            for (k in 0..32) {
                val right = ByteArray(k)
                for (i in 0 until loops) {
                    U256Tests.SR.nextBytes(left)
                    U256Tests.SR.nextBytes(right)

                    if (j == 32)
                        left[0] = left[0].and(0x7f)
                    if (k == 32)
                        right[0] = right[0].and(0x7f)

                    if (j >= 24 && k >= 24 && i == 3) {
                        for (x in 4 until j)
                            left[x] = 0
                        for (x in 4 until k)
                            right[x] = 0
                    }

                    val leftNeg = U256Tests.SR.nextBoolean()
                    val rightNeg = U256Tests.SR.nextBoolean()

                    var l = if (i == 0) {
                        BigInteger.ZERO
                    } else {
                        BigInteger(
                            if (leftNeg) {
                                -1
                            } else {
                                1
                            }, left
                        )
                    }
                    var r = if (i == 1) {
                        l
                    } else {
                        BigInteger(
                            if (rightNeg) {
                                -1
                            } else {
                                1
                            }, right
                        )
                    }

                    if (k == 4 && i == 2) {
                        l = r + r
                    }


                    try {
                        testSinglePair(op, l, r)
                    } catch (e: Exception) {
                        println("exception found l = $l r = $r, op = $op")
                        throw e
                    }
                }
            }
        }
    }
}