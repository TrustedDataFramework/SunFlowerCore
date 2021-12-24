package org.tdf.evm

import java.nio.charset.StandardCharsets

const val SELECTOR_SIZE = 4

fun ByteArray.hex(start: Int = 0, end: Int = this.size): String {
    return "0x" + this.sliceArray(start until kotlin.math.min(end, this.size)).joinToString("") {
        java.lang.String.format("%02x", it)
    }
}

class RevertException(contract: ByteArray, data: ByteArray, digest: Digest, lastCodeSize: ByteArray) : RuntimeException(msgOf(contract, data, digest, lastCodeSize)) {
    companion object {
        fun msgOf(contract: ByteArray, data: ByteArray, digest: Digest, lastCodeSize: ByteArray): String {
            if (data.size < SELECTOR_SIZE) {
                return "err: " + contract.hex() + " last empty code size= " + lastCodeSize.hex()
            }

            val selector = lazy {
                val dg = ByteArray(SlotUtils.SLOT_BYTE_ARRAY_SIZE)
                val src = "Error(string)".ascii()
                digest.digest(src, 0, src.size, dg, 0)
                dg.sliceArray(0 until SELECTOR_SIZE)
            }

            if (!data.sliceArray(0 until SELECTOR_SIZE).contentEquals(selector.value)) {
                System.err.println("selector != Error(string)")
                return "selector != Error(string) err: " + contract.hex() + " last empty code size = " + lastCodeSize.hex()
            }

            var off = data.int(SELECTOR_SIZE, SlotUtils.SLOT_BYTE_ARRAY_SIZE)
            val len = data.int(SELECTOR_SIZE + off, SlotUtils.SLOT_BYTE_ARRAY_SIZE)
            off += SELECTOR_SIZE + SlotUtils.SLOT_BYTE_ARRAY_SIZE
            return contract.hex() + ": " + String(data.sliceArray(off until off + len), StandardCharsets.UTF_8)
        }
    }
}