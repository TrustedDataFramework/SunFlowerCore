package org.tdf.sunflower.p2pv2.p2p

import com.github.salpadding.rlpstream.RlpBuffer
import com.github.salpadding.rlpstream.RlpWritable
import com.github.salpadding.rlpstream.StreamId
import com.github.salpadding.rlpstream.annotation.RlpCreator
import com.github.salpadding.rlpstream.annotation.RlpProps
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.p2pv2.P2pMessageCodes
import org.tdf.sunflower.p2pv2.client.Capability
import org.tdf.sunflower.p2pv2.message.Message
import org.tdf.sunflower.p2pv2.message.ReasonCode

abstract class P2pMessage(command: P2pMessageCodes) : Message(command)

@RlpProps("p2pVersion", "clientId", "capabilities", "listenPort", "peerId")
class HelloMessage @RlpCreator constructor(
    var p2pVersion: Int,
    var clientId: String = "",
    capabilities: Array<Capability>,
    var listenPort: Int = 0,
    val peerId: ByteArray
) : P2pMessage(P2pMessageCodes.HELLO) {

    constructor(
        p2pVersion: Int,
        clientId: String = "",
        capabilities: Array<Capability>,
        listenPort: Int = 0,
        peerId: String
    ) : this(p2pVersion, clientId, capabilities, listenPort, HexBytes.decode(peerId))


    /**
     * A peer-network capability code, readable ASCII and 3 letters.
     * Currently only "eth", "shh" and "bzz" are known.
     */
    var capabilities = capabilities.toList()

    val peerIdHex: String
        get() = HexBytes.encode(peerId)

    override fun toString(): String {
        return "HelloMessage(p2pVersion=$p2pVersion, clientId='$clientId', listenPort=$listenPort, capabilities=$capabilities, peerIdHex='$peerIdHex')"
    }


}


class PingMessage @RlpCreator constructor() : P2pMessage(P2pMessageCodes.PING), RlpWritable {
    companion object {
        private val FIXED_PAYLOAD = HexBytes.decode("C0")
    }

    override fun writeToBuf(buf: RlpBuffer): Int {
        return buf.writeRaw(FIXED_PAYLOAD)
    }
}

class PongMessage @RlpCreator constructor() : P2pMessage(P2pMessageCodes.PONG), RlpWritable {
    companion object {
        private val FIXED_PAYLOAD = HexBytes.decode("C0")
    }

    override fun writeToBuf(buf: RlpBuffer): Int {
        return buf.writeRaw(FIXED_PAYLOAD)
    }
}

class DisconnectMessage(var reason: ReasonCode = ReasonCode.UNKNOWN) : P2pMessage(P2pMessageCodes.DISCONNECT),
    RlpWritable {
    companion object {
        @JvmStatic
        @RlpCreator
        fun fromRlpStream(bin: ByteArray, streamId: Long): DisconnectMessage {
            val li = StreamId.asList(bin, streamId, 1)
            if (li.size() > 0)
                return DisconnectMessage(ReasonCode.fromInt(li.intAt(0)))
            return DisconnectMessage()
        }
    }

    override fun writeToBuf(buf: RlpBuffer): Int {
        return buf.writeList(reason.reason)
    }
}

class GetPeersMessage : P2pMessage(P2pMessageCodes.GET_PEERS), RlpWritable {


    companion object {
        /**
         * GetPeers message is always a the same single command payload
         */
        val encoded: ByteArray = HexBytes.decode("C104")
    }

    override fun writeToBuf(buf: RlpBuffer): Int {
        return buf.writeRaw(encoded)
    }
}