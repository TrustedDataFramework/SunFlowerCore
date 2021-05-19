package org.tdf.sunflower.p2pv2.rlpx

import java.io.ByteArrayInputStream
import java.io.InputStream

data class Frame(val type: Int, val size: Int, val stream: InputStream) {
    var contextId: Int = -1
    var totalFrameSize: Int = -1

    constructor(type: Int, payload: ByteArray) : this(type, payload.size, ByteArrayInputStream(payload))

    companion object {
        fun fromPayload(type: Int, payload: ByteArray): Frame {
            return Frame(type, payload.size, ByteArrayInputStream(payload))
        }
    }
}