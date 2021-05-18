package org.tdf.sunflower.p2pv2

import java.io.ByteArrayInputStream
import java.io.InputStream

data class Frame(val type: Int, val size: Int, val payload: InputStream) {
    companion object {
        fun fromPayload(type: Int, payload: ByteArray): Frame{
            return Frame(type, payload.size, ByteArrayInputStream(payload))
        }
    }
}