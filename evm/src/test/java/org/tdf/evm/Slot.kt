package org.tdf.evm

import org.tdf.evm.SlotUtils.MAX_BYTE_ARRAY_SIZE
import java.math.BigInteger
import java.util.*

fun IntArray.reset() {
    Arrays.fill(this, 0)
}

fun IntArray.copyFrom(bytes: ByteArray) {
    SlotUtils.copyFrom(this, 0, bytes)
}

fun IntArray.copyFrom(n: BigInteger) {
    SlotUtils.copyFrom(this, 0, n)
}


fun IntArray.copyFrom(n: Int) {
    SlotUtils.copyFrom(this, 0, n)
}

fun IntArray.toByteArray(): ByteArray {
    val r = ByteArray(MAX_BYTE_ARRAY_SIZE)
    SlotUtils.encodeBE(this, 0, r, 0)
    return r
}

fun IntArray.toBigInt(): BigInteger {
    return SlotUtils.toBigInt(this, 0)
}

fun IntArray.add(slot: IntArray) {
    SlotUtils.add(this, 0, slot, 0, this, 0)
}

fun IntArray.sub(slot: IntArray) {
    SlotUtils.subMut(this, 0, slot, 0, this, 0)
}