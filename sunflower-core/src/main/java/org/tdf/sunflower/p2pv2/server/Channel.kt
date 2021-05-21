package org.tdf.sunflower.p2pv2.server

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPipeline
import org.tdf.sunflower.p2pv2.Node
import org.tdf.sunflower.p2pv2.message.ReasonCode
import org.tdf.sunflower.p2pv2.p2p.HelloMessage
import org.tdf.sunflower.p2pv2.rlpx.FrameCodec
import org.tdf.sunflower.p2pv2.rlpx.discover.NodeStatistics
import java.io.IOException
import java.net.InetSocketAddress

interface Channel {
    fun init(pipeline: ChannelPipeline, remoteId: String, discoveryMode: Boolean, channelManager: ChannelManager)
    var inetSocketAddress: InetSocketAddress?
    val discoveryMode: Boolean
    /**
     * Set node and register it in NodeManager if it is not registered yet.
     */
    fun initWithNode(nodeId: ByteArray, remotePort: Int = inetSocketAddress!!.port)


    val peerId: String

    val peerIdShort: String

    val node: Node?

    fun disconnect(reason: ReasonCode)

    /**
     * finish handshake, combine frame codec with message codec
     */
    fun finishHandshake(ctx: ChannelHandlerContext, frameCodec: FrameCodec, helloRemote: HelloMessage)

    val nodeStatistics: NodeStatistics

    fun sendHelloMessage(ctx: ChannelHandlerContext, frameCodec: FrameCodec, nodeId: String)

}