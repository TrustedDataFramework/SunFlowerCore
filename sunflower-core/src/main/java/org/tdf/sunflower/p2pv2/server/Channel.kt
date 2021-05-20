package org.tdf.sunflower.p2pv2.server

import io.netty.channel.ChannelPipeline
import java.net.InetSocketAddress

interface Channel {
    fun init(pipeline: ChannelPipeline, remoteId: String, discoveryMode: Boolean, channelManager: ChannelManager)
    var inetSocketAddress: InetSocketAddress?

    /**
     * Set node and register it in NodeManager if it is not registered yet.
     */
    fun initWithNode(nodeId: ByteArray?, remotePort: Int);

    fun initWithNode(nodeId: ByteArray?);
}