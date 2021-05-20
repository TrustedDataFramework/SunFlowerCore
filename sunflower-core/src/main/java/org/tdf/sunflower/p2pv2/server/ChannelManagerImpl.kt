package org.tdf.sunflower.p2pv2.server

import io.netty.channel.ChannelPipeline
import org.apache.commons.collections4.map.LRUMap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.AppConfig
import java.net.InetAddress
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList


// TODO: add node into node manager
@Component
class ChannelManagerImpl @Autowired constructor
    (val server: PeerServerV2, val cfg: AppConfig)
    : ChannelManager{


    override var acceptingNewPeers: Boolean = false

    override fun init(
        pipeline: ChannelPipeline,
        remoteId: String,
        discoveryMode: Boolean,
        channelManager: ChannelManager
    ) {
        TODO("Not yet implemented")
    }

    override fun add(ch: Channel) {
        TODO("Not yet implemented")
    }

    override fun notifyDisconnect(ch: Channel) {
        TODO("Not yet implemented")
    }

    override fun isAddressInQueue(peerAddr: InetAddress): Boolean {
        TODO("Not yet implemented")
    }

    override fun isRecentlyDisconnected(peerAddr: InetAddress): Boolean {
        TODO("Not yet implemented")
    }

}