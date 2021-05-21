package org.tdf.sunflower.p2pv2.message

interface MessageFactory {
    fun create(code: Int, encoded: ByteArray): Message
}