package org.tdf.common.util

import com.github.salpadding.rlpstream.Rlp
import java.math.BigInteger
import java.nio.charset.StandardCharsets


/**
 * decode from rlp
 */
fun <T> HexBytes.decode(clazz: Class<T>): T {
    return Rlp.decode(this.bytes, clazz)
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

fun String.ascii(): ByteArray {
    return this.toByteArray(StandardCharsets.US_ASCII)
}

fun Long.bytes(): ByteArray {
    return ByteUtil.longToBytesNoLeadZeroes(this)
}