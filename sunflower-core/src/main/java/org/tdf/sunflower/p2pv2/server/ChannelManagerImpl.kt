package org.tdf.sunflower.p2pv2.server

import io.netty.channel.ChannelPipeline
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.AppConfig
import org.tdf.sunflower.p2pv2.Loggers
import org.tdf.sunflower.p2pv2.Node
import org.tdf.sunflower.p2pv2.client.PeerClient
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList


// TODO: add node into node manager
@Component
class ChannelManagerImpl @Autowired constructor
    (val server: PeerServerV2, val cfg: AppConfig, val client: PeerClient) : ChannelManager, Loggers {
    private val newPeers: MutableList<Channel> = CopyOnWriteArrayList()
    private val MAX_NEW_PEERS = 128
    val INBOUND_CONNECTION_BAN_TIMEOUT = 120 * 1000

    private val _activePeers: MutableMap<HexBytes, Channel> = ConcurrentHashMap()

    override val activePeers: List<Channel>
        get() = _activePeers.values.toList()

    override val acceptingNewPeers: Boolean
        get() = newPeers.size < Math.max(cfg.maxActivePeers, MAX_NEW_PEERS)

    override fun connect(node: Node) {
        if (net.isTraceEnabled) net.trace(
            "Peer {}: initiate connection",
            node.hexIdShort
        )
        if (nodesInUse.contains(node.hexId)) {
            if (net.isTraceEnabled) net.trace(
                "Peer {}: connection already initiated",
                node.hexIdShort
            )
            return
        }
        client.connectAsync(node.host, node.port, node.hexId)
    }

    override val nodesInUse: Set<String>
        get() {
            val ids: MutableSet<String> = HashSet()
            for (peer in activePeers) {
                ids.add(peer.peerId)
            }
            for (peer in newPeers) {
                ids.add(peer.peerId)
            }
            return ids
        }

    override fun init(
        pipeline: ChannelPipeline,
        remoteId: String,
        discoveryMode: Boolean,
        channelManager: ChannelManager
    ) {

    }

    override fun add(ch: Channel) {

    }

    override fun notifyDisconnect(ch: Channel) {

    }

    override fun isAddressInQueue(peerAddr: InetAddress): Boolean {
        return false
    }

    override fun isRecentlyDisconnected(peerAddr: InetAddress): Boolean {
        return false
    }

}