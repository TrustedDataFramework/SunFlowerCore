package org.tdf.sunflower.p2pv2

import org.tdf.common.util.HexBytes
import org.tdf.rlpstream.RlpEncodable

sealed class Message(
    val code: MessageCode
    ) : RlpEncodable{

    protected var parsed = false
}

class PingMessage: Message(MessageCode.PING) {
    private val encoded = FIXED_PAYLOAD;

    companion object {
        private val FIXED_PAYLOAD = HexBytes.decode("C0")
    }

    override fun getEncoded(): ByteArray {
        return encoded
    }
};


