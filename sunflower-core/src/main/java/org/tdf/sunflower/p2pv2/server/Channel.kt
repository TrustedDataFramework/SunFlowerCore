package org.tdf.sunflower.p2pv2.server

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPipeline
import io.netty.handler.timeout.ReadTimeoutHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.tdf.rlpstream.Rlp
import org.tdf.sunflower.AppConfig
import org.tdf.sunflower.p2pv2.MessageQueue
import org.tdf.sunflower.p2pv2.Node
import org.tdf.sunflower.p2pv2.WireTrafficStats
import org.tdf.sunflower.p2pv2.message.ReasonCode
import org.tdf.sunflower.p2pv2.message.StaticMessages
import org.tdf.sunflower.p2pv2.p2p.HelloMessage
import org.tdf.sunflower.p2pv2.rlpx.Frame
import org.tdf.sunflower.p2pv2.rlpx.FrameCodec
import org.tdf.sunflower.p2pv2.rlpx.MessageCodec
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

@Component @Scope("prototype")
class Channel @Autowired constructor(
    val mq: MessageQueue,
    val cfg: AppConfig,
    val stats: WireTrafficStats,
    val staticMessages: StaticMessages,
    val messageCodec: MessageCodec
) {
    var inetSocketAddress: InetSocketAddress? = null
    var discoveryMode = false
    var isActive = false
    var channelManager: ChannelManager? = null
    var remoteId: String = ""
    var node: Node? = null

    fun init(pipeline: ChannelPipeline, remoteId: String, discoveryMode: Boolean, channelManager: ChannelManager) {
        this.channelManager = channelManager
        this.remoteId = remoteId
        isActive = remoteId.isNotEmpty()
        pipeline.addLast(
            "readTimeoutHandler",
            ReadTimeoutHandler(cfg.peerChannelReadTimeout, TimeUnit.SECONDS)
        )
        pipeline.addLast(stats.tcp)
        // handle handshake here
//        pipeline.addLast("handshakeHandler", handshakeHandler)
        this.discoveryMode = discoveryMode
        if (discoveryMode) {
            // temporary key/nodeId to not accidentally smear our reputation with
            // unexpected disconnect
//            handshakeHandler.generateTempKey();
        }
//        handshakeHandler.setRemoteId(remoteId, this)
//        messageCodec.setChannel(this)
//        msgQueue.setChannel(this)
//        p2pHandler.setMsgQueue(msgQueue)
//        messageCodec.setP2pMessageFactory(P2pMessageFactory())
//        shhHandler.setMsgQueue(msgQueue)
//        messageCodec.setShhMessageFactory(ShhMessageFactory())
//        bzzHandler.setMsgQueue(msgQueue)
//        messageCodec.setBzzMessageFactory(BzzMessageFactory())
    }

    /**
     * Set node and register it in NodeManager if it is not registered yet.
     */
    fun initWithNode(nodeId: ByteArray, remotePort: Int) {
        node = Node(nodeId, inetSocketAddress!!.hostString, remotePort)
    }

    fun initWithNode(nodeId: ByteArray) {
        initWithNode(nodeId, inetSocketAddress!!.port)
    }


    fun sendHelloMessage(
        ctx: ChannelHandlerContext, frameCodec: FrameCodec,
        nodeId: String
    ) {
        val helloMessage: HelloMessage = staticMessages.createHelloMessage(nodeId)
        val byteBufMsg = ctx.alloc().buffer()

        frameCodec.writeFrame(
            Frame(helloMessage.code, Rlp.encode(helloMessage)),
            byteBufMsg
        )

        ctx.writeAndFlush(byteBufMsg).sync()
        log.debug(
            "To:   {}    Send:  {}",
            ctx.channel().remoteAddress(),
            helloMessage
        )
//        getNodeStatistics().rlpxOutHello.add()
    }

    fun disconnect(reason: ReasonCode) {
//        getNodeStatistics().nodeDisconnectedLocal(reason)
//        msgQueue.disconnect(reason)
    }


    fun publicRLPxHandshakeFinished(
        ctx: ChannelHandlerContext, frameCodec: FrameCodec,
        helloRemote: HelloMessage
    ) {
        log.debug(
            "publicRLPxHandshakeFinished with " + ctx.channel().remoteAddress()
        )
        messageCodec.setSupportChunkedFrames(false)
        val frameCodecHandler = FrameCodecHandler(frameCodec, this)
        ctx.pipeline().addLast("medianFrameCodec", frameCodecHandler)
        if (SnappyCodec.isSupported(Math.min(config.defaultP2PVersion(), helloRemote.getP2PVersion()))) {
            ctx.pipeline().addLast("snappyCodec", SnappyCodec(this))
            org.ethereum.net.server.Channel.logger.debug("{}: use snappy compression", ctx.channel())
        }
        ctx.pipeline().addLast("messageCodec", messageCodec)
        ctx.pipeline().addLast(Capability.P2P, p2pHandler)
        p2pHandler.setChannel(this)
        p2pHandler.setHandshake(helloRemote, ctx)
        getNodeStatistics().rlpxHandshake.add()
    }
    companion object {
        val log: Logger = LoggerFactory.getLogger("net")
    }
}