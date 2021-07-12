package org.tdf.common.util

import com.github.salpadding.rlpstream.Rlp
import org.tdf.common.types.Constants
import org.tdf.common.types.Uint256
import java.math.BigInteger
import java.nio.charset.StandardCharsets


/**
 * abi encoded selector part
 */
fun ByteArray.selector(): ByteArray {
    return sliceArray(0 until 4)
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
    if(this.size != Constants.WORD_SIZE)
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
    return if (this.startsWith("0x")) {
        Uint256.of(this.substring(2), 16)
    } else {
        Uint256.of(this)
    }
}

fun String.ascii(): ByteArray {
    return this.toByteArray(StandardCharsets.US_ASCII)
}

fun Long.bytes(): ByteArray {
    return ByteUtil.longToBytesNoLeadZeroes(this)
}

fun ByteArray.u256(): Uint256 {
    return Uint256.of(this)
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

fun BigInteger.bytes(i: Int): ByteArray {
    return BigIntegers.asUnsignedByteArray(i, this)
}