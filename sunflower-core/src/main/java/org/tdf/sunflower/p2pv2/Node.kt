package org.tdf.sunflower.p2pv2

import com.github.salpadding.rlpstream.Rlp
import com.github.salpadding.rlpstream.RlpList
import org.spongycastle.util.encoders.Hex
import org.tdf.common.crypto.ECKey
import org.tdf.common.util.ByteUtil.*
import org.tdf.common.util.CommonUtils
import org.tdf.common.util.HashUtil.sha3
import org.tdf.common.util.RLPUtil
import java.io.Serializable
import java.net.URI
import java.net.URISyntaxException

class Node : Serializable, Loggers {
    var id: ByteArray
    var host: String = ""
    var port = 0

    /**
     * @return true if this node is endpoint for discovery loaded from config
     */
    // discovery endpoint doesn't have real nodeId for example
    var isDiscoveryNode = false

    constructor(enodeURL: String) {
        try {
            val uri = URI(enodeURL)
            if (uri.scheme != "enode") {
                throw RuntimeException("expecting URL in the format enode://PUBKEY@HOST:PORT")
            }
            id = Hex.decode(uri.userInfo)
            host = uri.host
            port = uri.port
        } catch (e: URISyntaxException) {
            throw RuntimeException("expecting URL in the format enode://PUBKEY@HOST:PORT", e)
        }
    }

    constructor(id: ByteArray, host: String, port: Int) {
        this.id = id
        this.host = host
        this.port = port
    }

    /**
     * Instantiates node from RLP list containing node data.
     * @throws IllegalArgumentException if node id is not a valid EC point.
     */
    constructor(nodeRLP: RlpList) {
        // host
        val hostB: ByteArray = nodeRLP.bytesAt(0)
        // udp port
        val portB: ByteArray = nodeRLP.bytesAt(1)
        // full -> idB = id
        // short -> idb = tcp port
        val idB: ByteArray = if (nodeRLP.size() > 3) {
            nodeRLP.bytesAt(3)
        } else {
            net.error("short node found")
            nodeRLP.bytesAt(2)
        }
        val port: Int = byteArrayToInt(portB)
        host = bytesToIp(hostB)
        this.port = port

        // a tricky way to check whether given data is a valid EC point or not
        id = ECKey.fromNodeId(idB).nodeId
    }

    constructor(rlp: ByteArray) : this(RLPUtil.decodePartial(rlp, 0))

    val hexId: String
        get() = Hex.toHexString(id)
    val hexIdShort: String
        get() = CommonUtils.getNodeIdShort(hexId)

    /**
     * Full RLP
     * [host, udpPort, tcpPort, nodeId]
     * @return RLP-encoded node data
     */
    val fullRlp: ByteArray
        get() {
            val rlphost: ByteArray = Rlp.encodeBytes(hostToBytes(host))
            val rlpUDPPort: ByteArray = Rlp.encodeInt(port)
            val rlpTCPPort: ByteArray = Rlp.encodeInt(port)
            val rlpId: ByteArray = Rlp.encodeBytes(id)
            return Rlp.encodeElements(rlphost, rlpUDPPort, rlpTCPPort, rlpId)
        }

    /**
     * RLP without nodeId
     * [host, udpPort, tcpPort]
     * @return RLP-encoded node data
     */
    val briefRlp: ByteArray
        get() {
            val rlphost: ByteArray = Rlp.encodeBytes(hostToBytes(host))
            val rlpTCPPort: ByteArray = Rlp.encodeInt(port)
            val rlpUDPPort: ByteArray = Rlp.encodeInt(port)
            return Rlp.encodeElements(rlphost, rlpUDPPort, rlpTCPPort)
        }

    override fun toString(): String {
        return "Node{" +
                " host='" + host + '\'' +
                ", port=" + port +
                ", id=" + toHexString(id) +
                '}'
    }

    override fun hashCode(): Int {
        return this.toString().hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (other === this) {
            return true
        }
        return if (other is Node) {
            other.id.contentEquals(id)
        } else false
    }

    companion object {
        private const val serialVersionUID = -4267600517925770636L

        /**
         * - create Node instance from enode if passed,
         * - otherwise fallback to random nodeId, if supplied with only "address:port"
         * NOTE: validation is absent as method is not heavily used
         */
        fun instanceOf(addressOrEnode: String): Node {
            try {
                val uri = URI(addressOrEnode)
                if (uri.scheme == "enode") {
                    return Node(addressOrEnode)
                }
            } catch (e: URISyntaxException) {
                // continue
            }
            val generatedNodeKey: ECKey = ECKey.fromPrivate(sha3(addressOrEnode.toByteArray()))
            val generatedNodeId = Hex.toHexString(generatedNodeKey.nodeId)
            val node = Node("enode://$generatedNodeId@$addressOrEnode")
            node.isDiscoveryNode = true
            return node
        }
    }
}
