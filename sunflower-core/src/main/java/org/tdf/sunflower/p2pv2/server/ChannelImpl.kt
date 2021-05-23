package org.tdf.sunflower.p2pv2.server

import com.github.salpadding.rlpstream.Rlp
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPipeline
import io.netty.handler.timeout.ReadTimeoutHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.AppConfig
import org.tdf.sunflower.p2pv2.Loggers
import org.tdf.sunflower.p2pv2.MessageQueue
import org.tdf.sunflower.p2pv2.Node
import org.tdf.sunflower.p2pv2.WireTrafficStats
import org.tdf.sunflower.p2pv2.client.Capability
import org.tdf.sunflower.p2pv2.eth.EthVersion
import org.tdf.sunflower.p2pv2.message.ReasonCode
import org.tdf.sunflower.p2pv2.message.StaticMessages
import org.tdf.sunflower.p2pv2.p2p.HelloMessage
import org.tdf.sunflower.p2pv2.p2p.P2pHandler
import org.tdf.sunflower.p2pv2.rlpx.*
import org.tdf.sunflower.p2pv2.rlpx.discover.NodeManager
import org.tdf.sunflower.p2pv2.rlpx.discover.NodeStatistics
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

@Component
@Scope("prototype")
class ChannelImpl @Autowired constructor(
    private val mq: MessageQueue,
    private val handshake: HandshakeHandler,
    private val cfg: AppConfig,
    private val stats: WireTrafficStats,
    private val staticMessages: StaticMessages,
    private val nodeManager: NodeManager,
    private val codec: MessageCodec,
    private val p2pHandler: P2pHandler
) : Channel, Loggers {
    init {
        mq.channel = this
    }

    override var discoveryMode: Boolean = false
        private set

    override val isDisconnected = false
    // set when initWithNode
    private var _nodeStatistics: NodeStatistics? = null
    override val nodeStatistics: NodeStatistics
        get() = _nodeStatistics!!

    private var _node: Node? = null
    override val node: Node
        get() = _node!!

    override var capabilities: List<Capability>
        get() = codec.capabilities
        set(value) {
            codec.capabilities = value
        }

    override fun disconnect(reason: ReasonCode) {
        nodeStatistics.nodeDisconnectedLocal(reason)
        mq.disconnect(reason)
    }

    override fun finishHandshake(ctx: ChannelHandlerContext, frameCodec: FrameCodec, helloRemote: HelloMessage) {
        dev.info("channel impl finish handshake")
        mq.supportChunkedFrames = false
        val handler = FrameCodecHandler(frameCodec, this)
        ctx.pipeline().addLast("medianFrameCodec", handler)

        if (SnappyCodec.isSupported(Math.min(cfg.defaultP2PVersion, helloRemote.p2pVersion))) {
            dev.info("snappy codec added")
            ctx.pipeline().addLast("snappyCodec", SnappyCodec(this))
            net.debug("${ctx.channel()}: use snappy compression")
        }

        ctx.pipeline().addLast("messageCodec", codec)
        ctx.pipeline().addLast(Capability.P2P, p2pHandler)

        p2pHandler.channel = this
        p2pHandler.setHandshake(helloRemote, ctx)
        nodeStatistics.rlpxHandshake.add()
    }

    var remoteId: String = ""
        private set

    var active: Boolean = false
        private set

    override val peerId: String
        get() = _node?.hexId ?: "<null>"

    override val peerIdShort: String
        get() {
            return if (_node == null) {
                val v = remoteId
                v.substring(0, Math.max(v.length, 8))
            } else {
                node.hexIdShort
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
        this.discoveryMode = discoveryMode
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
        codec.channel = this
        mq.channel = this
        p2pHandler.mq = mq
    }

    private var _inetSocketAddress: InetSocketAddress? = null

    // set when channel active
    override var inetSocketAddress
        get() = _inetSocketAddress!!
        set(v) {
            _inetSocketAddress = v
        }


    override fun initWithNode(nodeId: ByteArray, remotePort: Int) {
        dev.info("init with node nodeId = ${HexBytes.fromBytes(nodeId)} remote port = ${remotePort}, remote host = ${inetSocketAddress.hostName}")
        _node = Node(nodeId, inetSocketAddress.hostString, remotePort)
        _nodeStatistics = nodeManager.getNodeStatistics(node)
    }

    override fun sendHelloMessage(ctx: ChannelHandlerContext, frameCodec: FrameCodec, nodeId: String) {
        val hello = staticMessages.createHelloMessage(nodeId)
        val byteBufMsg = ctx.alloc().buffer()
        frameCodec.writeFrame(Frame(hello.code, Rlp.encode(hello)), byteBufMsg)
        ctx.writeAndFlush(byteBufMsg).sync()

        if (net.isDebugEnabled)
            net.debug(
                "To:   {}    Send:  {}",
                ctx.channel().remoteAddress(),
                hello
            )
        nodeStatistics.rlpxOutHello.add()
    }

    override fun activateEth(ctx: ChannelHandlerContext, version: EthVersion) {
        dev.info("eth activated")
    }
}