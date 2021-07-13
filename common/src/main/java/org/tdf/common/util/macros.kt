package org.tdf.common.util

import com.github.salpadding.rlpstream.Rlp
import org.tdf.common.types.Constants
import org.tdf.common.types.Constants.WORD_SIZE
import org.tdf.common.types.Uint256
import java.math.BigInteger
import java.nio.charset.StandardCharsets


/**
 * abi encoded selector part
 */
fun ByteArray.selector(): ByteArray {
    return sliceArray(0 until 4)
}

fun Long.bn(): BigInteger {
    return BigInteger.valueOf(this)
}

/**
 * abi encoded without selector
 */
fun ByteArray.unselect(): ByteArray {
    return sliceArray(4 until size)
}

/**
 * decode from rlp
 */
fun <T> HexBytes.decode(clazz: Class<T>): T {
    return Rlp.decode(this.bytes, clazz)
}

fun HexBytes.h256(): H256 {
    if (this.size != Constants.WORD_SIZE)
        throw RuntimeException("invalid word size")
    return this
}

/**
 * decode from rlp
 */
fun <T> ByteArray.decode(clazz: Class<T>): T {
    return Rlp.decode(this, clazz)
}

fun ByteArray.hex(): HexBytes {
    return HexBytes.fromBytes(this)
}

fun ByteArray.sha3(): ByteArray {
    return HashUtil.sha3(this)
}

fun HexBytes.sha3(): HexBytes {
    return HexBytes.fromBytes(HashUtil.sha3(this.bytes))
}

fun Any?.rlp(): ByteArray {
    return Rlp.encode(this)
}


fun String.hex(): HexBytes {
    return HexBytes.fromHex(this)
}

fun String.u256(): Uint256 {
    return Uint256.of(this.hex().bytes)
}

fun String.ascii(): ByteArray {
    return this.toByteArray(StandardCharsets.US_ASCII)
}

fun Long.bytes(): ByteArray {
    return ByteUtil.longToBytesNoLeadZeroes(this)
}

fun Long.bytes8(): ByteArray {
    return BigEndian.encodeInt64(this)
}

fun ByteArray.u256(): Uint256 {
    return Uint256.of(this)
}

fun ByteArray.long(): Long {
    return BigEndian.decodeInt64(this, 0)
}

fun ByteArray.bn(): BigInteger {
    return BigInteger(1, this)
}

fun Long.u256(): Uint256 {
    return Uint256.of(this)
}

fun BigInteger.u256(): Uint256 {
    return Uint256.of(this)
}

fun BigInteger.bytes(): ByteArray {
    return BigIntegers.asUnsignedByteArray(this)
}

fun BigInteger.bytes32(): ByteArray {
    return BigIntegers.asUnsignedByteArray(WORD_SIZE, this)
}