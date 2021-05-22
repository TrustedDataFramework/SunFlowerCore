package org.tdf.sunflower.p2pv2.message

import org.tdf.sunflower.p2pv2.MessageCode


interface MessageDecoder<T> where T : MessageCode {
    fun decode(code: T, encoded: ByteArray): Message
}