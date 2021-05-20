package org.tdf.sunflower.p2pv2.server

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPipeline
import org.tdf.sunflower.p2pv2.Node
import org.tdf.sunflower.p2pv2.message.ReasonCode
import org.tdf.sunflower.p2pv2.p2p.HelloMessage
import org.tdf.sunflower.p2pv2.rlpx.FrameCodec
import java.net.InetSocketAddress

interface Channel {
    fun init(pipeline: ChannelPipeline, remoteId: String, discoveryMode: Boolean, channelManager: ChannelManager)
    var inetSocketAddress: InetSocketAddress?

    /**
     * Set node and register it in NodeManager if it is not registered yet.
     */
    fun initWithNode(nodeId: ByteArray?, remotePort: Int)

    fun initWithNode(nodeId: ByteArray?)

    val peerId: String

    val peerIdShort: String

    val node: Node?

    fun disconnect(reason: ReasonCode)

    /**
     * finish handshake, combine frame codec with message codec
     */
    fun finishHandshake(ctx: ChannelHandlerContext, frameCodec: FrameCodec, helloRemote: HelloMessage)
}