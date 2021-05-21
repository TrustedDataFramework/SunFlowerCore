package org.tdf.sunflower.p2pv2.p2p

import org.tdf.rlpstream.Rlp
import org.tdf.sunflower.p2pv2.P2pMessageCodes
import org.tdf.sunflower.p2pv2.message.MessageFactory
import org.tdf.sunflower.p2pv2.message.StaticMessages
import java.lang.RuntimeException

object P2PMessageFactory: MessageFactory{

    override fun create(code: Int, encoded: ByteArray): P2pMessage {
        val cmd = P2pMessageCodes.fromInt(code)
        val r: P2pMessage = when(cmd) {
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