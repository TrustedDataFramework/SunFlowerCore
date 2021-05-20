package org.tdf.sunflower.p2pv2.server

import io.netty.channel.ChannelPipeline
import org.tdf.sunflower.p2pv2.Node
import java.net.InetAddress

interface ChannelManager {
    fun init(pipeline: ChannelPipeline, remoteId: String, discoveryMode: Boolean, channelManager: ChannelManager)

    fun add(ch: Channel)

    fun notifyDisconnect(ch: Channel)

    fun isAddressInQueue(peerAddr: InetAddress): Boolean

    fun isRecentlyDisconnected(peerAddr: InetAddress): Boolean

    val acceptingNewPeers: Boolean

    fun connect(node: Node)

    val nodesInUse: Set<String>

    val activePeers: List<Channel>

}