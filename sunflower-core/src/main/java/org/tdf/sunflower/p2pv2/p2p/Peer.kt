package org.tdf.sunflower.p2pv2.p2p

import com.github.salpadding.rlpstream.RlpProps
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.p2pv2.client.Capability
import java.net.InetAddress

@RlpProps("ip", "port", "peerIdBytes", "capabilities")
class Peer(
    val address: InetAddress, val port: Int,
    val peerId: String, val capability: List<Capability>
) {

    val ip: ByteArray
        get() = address.address

    val peerIdBytes: ByteArray
        get() = HexBytes.decode(peerId)


    override fun hashCode(): Int {
        var result = peerId.hashCode()
        result = 31 * result + address.hashCode()
        result = 31 * result + port
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Peer
        return peerId == other.peerId || address == other.address
    }
}