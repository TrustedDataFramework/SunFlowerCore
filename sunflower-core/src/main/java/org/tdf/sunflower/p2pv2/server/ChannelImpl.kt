package org.tdf.sunflower.p2pv2.server

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPipeline
import io.netty.handler.timeout.ReadTimeoutHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.tdf.sunflower.AppConfig
import org.tdf.sunflower.p2pv2.MessageQueue
import org.tdf.sunflower.p2pv2.Node
import org.tdf.sunflower.p2pv2.WireTrafficStats
import org.tdf.sunflower.p2pv2.message.ReasonCode
import org.tdf.sunflower.p2pv2.message.StaticMessages
import org.tdf.sunflower.p2pv2.p2p.HelloMessage
import org.tdf.sunflower.p2pv2.rlpx.Frame
import org.tdf.sunflower.p2pv2.rlpx.FrameCodec
import org.tdf.sunflower.p2pv2.rlpx.HandshakeHandler
import org.tdf.sunflower.p2pv2.rlpx.MessageCodec
import org.tdf.sunflower.p2pv2.rlpx.discover.NodeStatisticsImpl
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

@Component
@Scope("prototype")
class ChannelImpl @Autowired constructor(
    private val mq: MessageQueue,
    private val handshake: HandshakeHandler,
    private val cfg: AppConfig,
    private val stats: WireTrafficStats,
    private val staticMessages: StaticMessages
    ) : Channel {
    init {
        mq.channel = this
    }

    override val nodeStatistics = NodeStatisticsImpl()

    override var node: Node? = null
        private set

    override fun disconnect(reason: ReasonCode) {
        nodeStatistics.nodeDisconnectedLocal(reason)
        mq.disconnect(reason)
    }

    override fun finishHandshake(ctx: ChannelHandlerContext, frameCodec: FrameCodec, helloRemote: HelloMessage) {
        TODO("Not yet implemented")
    }

    var remoteId: String = ""
        private set

    var active: Boolean = false
        private set

    override val peerId: String
        get() = node?.hexId ?: "<null>"

    override val peerIdShort: String
        get() {
            return if(node == null) {
                val v = remoteId ?: ""
                v.substring(0, Math.max(v.length, 8))
            } else {
                node!!.hexIdShort
            }
        }

    private var _channelManager: ChannelManager? = null

    val channelManager: ChannelManager
        get() = _channelManager!!

    override fun init(
        pipeline: ChannelPipeline,
        remoteId: String,
        discoveryMode: Boolean,
        channelManager: ChannelManager
    ) {
        _channelManager = channelManager
        this.remoteId = remoteId
        this.active = remoteId.isNotEmpty()
        pipeline.addLast(
            "readTimeoutHandler",
            ReadTimeoutHandler(cfg.peerChannelReadTimeout, TimeUnit.SECONDS)
        )
        pipeline.addLast(stats.tcp)
        pipeline.addLast("handshakeHandler", handshake)
        handshake.setRemote(remoteId, this)
        mq.channel = this
    }

    override var inetSocketAddress: InetSocketAddress? = null

    override fun initWithNode(nodeId: ByteArray?, remotePort: Int) {
        TODO("Not yet implemented")
    }

    override fun initWithNode(nodeId: ByteArray?) {
        TODO("Not yet implemented")
    }

    override fun sendHelloMessage(ctx: ChannelHandlerContext, frameCodec: FrameCodec, nodeId: String) {
        val hello = staticMessages.createHelloMessage(nodeId)
        val byteBufMsg = ctx.alloc().buffer()
//        frameCodec.writeFrame(Frame(hello))
    }
}