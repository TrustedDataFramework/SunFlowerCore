package org.tdf.common.types

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.tdf.common.types.Uint256.Uint256Deserializer
import org.tdf.common.types.Uint256.Uint256EncoderDecoder
import org.tdf.common.util.*
import org.tdf.rlp.*
import java.io.IOException
import java.lang.NullPointerException
import java.math.BigInteger
import java.util.*
import kotlin.math.sign

@JsonDeserialize(using = Uint256Deserializer::class)
@JsonSerialize(using = IntSerializer::class)
@RLPEncoding(
    Uint256EncoderDecoder::class
)
@RLPDecoding(Uint256EncoderDecoder::class)
class Uint256 private constructor(data: ByteArray) : Number() {
    private val data: ByteArray

    override fun toInt(): Int {
        return value().intValueExact()
    }

    override fun toLong(): Long {
        return value().longValueExact()
    }

    override fun toShort(): Short {
        return value().shortValueExact()
    }

    override fun toFloat(): Float {
        return value().toFloat()
    }

    override fun toDouble(): Double {
        return value().toDouble()
    }

    override fun toByte(): Byte {
        return value().byteValueExact()
    }

    override fun toChar(): Char {
        return value().toChar()
    }

    /**
     * Returns instance data
     * Actually copy of internal byte array is provided
     * in order to protect Uint256 immutability
     *
     * @return instance data
     */
    fun getData(): ByteArray {
        return data.copyOf(data.size)
    }

    val dataHex: HexBytes
        get() = HexBytes.fromBytes(getData())


    val noLeadZeroesData: ByteArray
        get() = getNoLeadZeroesData(data)

    fun value(): BigInteger {
        return BigInteger(1, data)
    }

    val isZero: Boolean
        get() = if (this === ZERO) true else this.compareTo(ZERO) == 0


    fun uncheckedPlus(word: Uint256): Uint256 {
        val result = value().add(word.value())
        return Uint256(ByteUtil.copyToArray(result.and(MAX_VALUE)))
    }

    operator fun plus(word: Uint256): Uint256 {
        val ret = uncheckedPlus(word)
        if (ret < this || ret < word) throw RuntimeException("unexpected exception: math overflow")
        return ret
    }

    operator fun div(word: Uint256): Uint256 {
        if (word.isZero) {
            throw RuntimeException("divided by zero")
        }
        return uncheckedDiv(word)
    }

    fun uncheckedDiv(word: Uint256): Uint256 {
        if (word.isZero) {
            return ZERO
        }
        val result = value().divide(word.value())
        return Uint256(ByteUtil.copyToArray(result.and(MAX_VALUE)))
    }

    fun uncheckedMinus(word: Uint256): Uint256 {
        val result = value().subtract(word.value())
        return Uint256(ByteUtil.copyToArray(result.and(MAX_VALUE)))
    }

    operator fun minus(word: Uint256): Uint256 {
        if (this < word) throw RuntimeException("math overflow")
        return uncheckedMinus(word)
    }

    fun uncheckedRem(word: Uint256): Uint256 {
        if (word.isZero) {
            return ZERO
        }
        val result = value().mod(word.value())
        return Uint256(ByteUtil.copyToArray(result.and(MAX_VALUE)))
    }

    operator fun rem(word: Uint256): Uint256 {
        if (word.isZero) {
            throw RuntimeException("divided by zero")
        }
        return uncheckedRem(word)
    }


    fun uncheckedTimes(word: Uint256): Uint256 {
        val result = value().multiply(word.value())
        return Uint256(ByteUtil.copyToArray(result.and(MAX_VALUE)))
    }

    operator fun times(word: Uint256): Uint256 {
        if (this.compareTo(ZERO) == 0) {
            return ZERO
        }
        val ret = uncheckedTimes(word)
        if (ret.div(this).compareTo(word) != 0) throw RuntimeException("SafeMath: multiplication overflow ")
        return ret
    }

    operator fun compareTo(o: Uint256?): Int {
        if (o == null)
            throw NullPointerException()
        val result = FastByteComparisons.compareTo(
            data, 0, data.size,
            o.data, 0, o.data.size
        )
        // Convert result into -1, 0 or 1 as is the convention
        return sign(result.toFloat()).toInt()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val uint256 = other as Uint256
        return this.compareTo(uint256) == 0
    }

    override fun hashCode(): Int {
        return data.contentHashCode()
    }


    class Uint256Deserializer : StdDeserializer<Uint256>(Uint256::class.java) {
        @Throws(IOException::class)
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Uint256 {
            val node = p.codec.readTree<JsonNode>(p)
            if (node.isNull) return ZERO
            val encoded = node.asText()
            if (encoded == null || encoded == "") {
                return ZERO
            }
            return if (encoded.startsWith("0x")) {
                of(encoded.substring(2), 16)
            } else of(encoded, 10)
        }
    }

    class Uint256Serializer : StdSerializer<Uint256>(Uint256::class.java) {
        @Throws(IOException::class)
        override fun serialize(value: Uint256, jgen: JsonGenerator, provider: SerializerProvider) {
            jgen.writeString(value.value().toString(10))
        }
    }

    class Uint256EncoderDecoder : RLPEncoder<Uint256>, RLPDecoder<Uint256> {
        override fun encode(uint256: Uint256): RLPElement {
            return RLPItem.fromBytes(uint256.noLeadZeroesData)
        }

        override fun decode(rlpElement: RLPElement): Uint256 {
            return of(rlpElement.asBytes())
        }
    }

    companion object {
        const val MAX_POW = 256
        val _2_256 = BigInteger.valueOf(2).pow(MAX_POW)
        val MAX_VALUE = _2_256.subtract(BigInteger.ONE)
        @JvmField
        val ZERO = Uint256(ByteArray(32))
        val ONE = of(1.toByte())
        fun of(pattern: String, radix: Int): Uint256 {
            val i = BigInteger(pattern, radix)
            if (i.compareTo(BigInteger.ZERO) < 0 || i.compareTo(MAX_VALUE) > 0) throw RuntimeException(
                "$pattern overflow"
            )
            return of(ByteUtil.bigIntegerToBytes(i, 32))
        }

        @JvmStatic
        fun of(v: BigInteger?): Uint256 {
            return of(BigIntegers.asUnsignedByteArray(v))
        }

        @JvmStatic
        fun of(data: ByteArray?): Uint256 {
            if (data == null || data.isEmpty()) {
                return ZERO
            }
            val leadingZeroBits = ByteUtil.numberOfLeadingZeros(data)
            val valueBits = 8 * data.size - leadingZeroBits
            if (valueBits <= 8) {
                if (data[data.size - 1] == (0).toByte()) return ZERO
                if (data[data.size - 1] == (1).toByte()) return ONE
            }
            return if (data.size == 32) Uint256(Arrays.copyOf(data, data.size)) else if (data.size <= 32) {
                val bytes = ByteArray(32)
                System.arraycopy(data, 0, bytes, 32 - data.size, data.size)
                Uint256(bytes)
            } else {
                throw RuntimeException(
                    String.format(
                        "Data word can't exceed 32 bytes: 0x%s",
                        ByteUtil.toHexString(data)
                    )
                )
            }
        }

        @JvmStatic
        fun of(data: String?): Uint256 {
            return of(HexBytes.decode(data!!))
        }

        @JvmStatic
        fun of(num: Byte): Uint256 {
            val bb = ByteArray(32)
            bb[31] = num
            return Uint256(bb)
        }

        @JvmStatic
        fun of(num: Int): Uint256 {
            return of(ByteUtil.intToBytes(num))
        }

        @JvmStatic
        fun of(num: Long): Uint256 {
            return of(ByteUtil.longToBytes(num))
        }

        @JvmStatic
        fun getNoLeadZeroesData(data: ByteArray): ByteArray {
            val firstNonZero = ByteUtil.firstNonZeroByte(data)
            return when (firstNonZero) {
                -1 -> ByteUtil.EMPTY_BYTE_ARRAY
                0 -> data
                else -> {
                    val result = ByteArray(data.size - firstNonZero)
                    System.arraycopy(data, firstNonZero, result, 0, data.size - firstNonZero)
                    result
                }
            }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val u = of(
                byteArrayOf(
                    255.toByte(),
                    255.toByte(),
                    255.toByte(),
                    255.toByte(),
                    255.toByte(),
                    255.toByte(),
                    255.toByte(),
                    255.toByte()
                )
            )
            println((u + u).value())
        }
    }

    /**
     * Unsafe private constructor
     * Doesn't guarantee immutability if byte[] contents are changed later
     * Use one of factory methods instead:
     * - [.of]
     * - [.of]
     * - [.of]
     * - [.of]
     *
     * @param data Byte Array[32] which is guaranteed to be immutable
     */
    init {
        if (data.size != 32) throw RuntimeException("Input byte array should have 32 bytes in it!")
        this.data = data
    }
}