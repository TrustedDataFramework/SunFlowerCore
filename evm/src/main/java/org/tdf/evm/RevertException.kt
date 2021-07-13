package org.tdf.evm

import java.nio.charset.StandardCharsets

const val SELECTOR_SIZE = 4

class RevertException(data: ByteArray, digest: Digest): RuntimeException(msgOf(data, digest)){
    companion object {
        fun msgOf(data: ByteArray, digest: Digest): String{
            if(data.size < SELECTOR_SIZE)
                return ""

            val selector = lazy {
                val dg = ByteArray(SlotUtils.SLOT_BYTE_ARRAY_SIZE)
                val src = "Error(string)".ascii()
                digest.digest(src, 0, src.size, dg, 0)
                dg.sliceArray(0 until SELECTOR_SIZE)
            }

            if(!data.sliceArray(0 until SELECTOR_SIZE).contentEquals(selector.value)) {
                return ""
            }

            var off = data.int(SELECTOR_SIZE, SlotUtils.SLOT_BYTE_ARRAY_SIZE)
            val len = data.int(SELECTOR_SIZE + off, SlotUtils.SLOT_BYTE_ARRAY_SIZE)
            off += SELECTOR_SIZE + SlotUtils.SLOT_BYTE_ARRAY_SIZE
            return String(data.sliceArray(off until off + len), StandardCharsets.UTF_8)
        }
    }
}