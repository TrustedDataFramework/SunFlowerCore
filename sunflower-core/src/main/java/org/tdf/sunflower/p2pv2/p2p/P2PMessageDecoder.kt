package org.tdf.sunflower.p2pv2.p2p

import com.github.salpadding.rlpstream.Rlp
import org.tdf.sunflower.p2pv2.Loggers
import org.tdf.sunflower.p2pv2.P2pMessageCodes
import org.tdf.sunflower.p2pv2.message.MessageDecoder
import org.tdf.sunflower.p2pv2.message.StaticMessages

object P2PMessageDecoder : MessageDecoder<P2pMessageCodes>, Loggers {
    override fun decode(code: P2pMessageCodes, encoded: ByteArray): P2pMessage {
        dev.info("create p2p message code = $code")
        val r: P2pMessage = when (code) {
            P2pMessageCodes.HELLO -> Rlp.decode(encoded, HelloMessage::class.java)
            P2pMessageCodes.DISCONNECT -> Rlp.decode(encoded, DisconnectMessage::class.java)
            P2pMessageCodes.PING -> StaticMessages.PING_MESSAGE
            P2pMessageCodes.PONG -> StaticMessages.PONG_MESSAGE
            P2pMessageCodes.GET_PEERS -> StaticMessages.GET_PEERS_MESSAGE
            P2pMessageCodes.PEERS -> throw RuntimeException("no such message")
            else -> throw RuntimeException("no such message")
        }
        return r
    }
}