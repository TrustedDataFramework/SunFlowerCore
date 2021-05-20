package org.tdf.sunflower.p2pv2.p2p


import org.tdf.common.util.HexBytes
import org.tdf.rlpstream.RlpCreator
import org.tdf.rlpstream.RlpEncodable
import org.tdf.rlpstream.RlpProps
import org.tdf.sunflower.p2pv2.P2pMessageCodes
import org.tdf.sunflower.p2pv2.client.Capability
import org.tdf.sunflower.p2pv2.message.Message

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