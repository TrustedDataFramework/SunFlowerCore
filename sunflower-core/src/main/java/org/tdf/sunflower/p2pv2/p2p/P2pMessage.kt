package org.tdf.sunflower.p2pv2.p2p


import org.tdf.common.util.HexBytes
import org.tdf.rlpstream.*
import org.tdf.sunflower.p2pv2.P2pMessageCodes
import org.tdf.sunflower.p2pv2.client.Capability
import org.tdf.sunflower.p2pv2.message.Message
import org.tdf.sunflower.p2pv2.message.ReasonCode

sealed class P2pMessage(command: P2pMessageCodes): Message(command)

@RlpProps("p2pVersion", "clientId", "capabilities", "listenPort", "peerId")
class HelloMessage @RlpCreator constructor(
    var p2pVersion: Int,
    var clientId: String = "",
    capabilities: Array<Capability>,
    var listenPort: Int = 0,
    var peerId: String = ""
) : P2pMessage(P2pMessageCodes.HELLO) {

    /**
     * A peer-network capability code, readable ASCII and 3 letters.
     * Currently only "eth", "shh" and "bzz" are known.
     */
    var capabilities = capabilities.toList()

}


class PingMessage @RlpCreator constructor() : Message(P2pMessageCodes.PING), RlpEncodable {

    companion object {
        private val FIXED_PAYLOAD = HexBytes.decode("C0")
    }
    override fun getEncoded(): ByteArray {
        return FIXED_PAYLOAD
    }
}

@RlpProps("reason")
class DisconnectMessage(var reason: ReasonCode = ReasonCode.UNKNOWN): Message(P2pMessageCodes.DISCONNECT) {
    companion object {
        @JvmStatic
        @RlpCreator
        fun fromRlpStream(bin: ByteArray, streamId: Long): DisconnectMessage{
            val li = RlpList(bin, streamId, 1)
            if(li.size() > 0)
                return DisconnectMessage(ReasonCode.fromInt(li.intAt(0)))
            return DisconnectMessage()
        }
    }
}