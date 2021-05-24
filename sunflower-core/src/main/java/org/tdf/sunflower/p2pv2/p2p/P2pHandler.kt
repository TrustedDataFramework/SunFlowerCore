package org.tdf.sunflower.p2pv2.p2p

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.TickerMode
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.tdf.sunflower.AppConfig
import org.tdf.sunflower.p2pv2.Loggers
import org.tdf.sunflower.p2pv2.MessageQueue
import org.tdf.sunflower.p2pv2.P2pMessageCodes
import org.tdf.sunflower.p2pv2.client.Capability
import org.tdf.sunflower.p2pv2.eth.EthVersion.Companion.fromCode
import org.tdf.sunflower.p2pv2.message.ReasonCode
import org.tdf.sunflower.p2pv2.message.StaticMessages
import org.tdf.sunflower.p2pv2.message.StaticMessages.Companion.PING_MESSAGE
import org.tdf.sunflower.p2pv2.server.Channel
import java.util.*
import java.util.concurrent.*

@Component
@Scope("prototype")
class P2pHandler(
    val cfg: AppConfig,
    val staticMessages: StaticMessages
) : SimpleChannelInboundHandler<P2pMessage>(), Loggers {
    val VERSION: Byte = 5

    private val logger = LoggerFactory.getLogger("net")

    private val pingTicker = ticker(
        TimeUnit.SECONDS.toMillis(cfg.p2pPingInterval.toLong()),
        TimeUnit.SECONDS.toMillis(2),
        mode = TickerMode.FIXED_DELAY
    )

    lateinit var mq: MessageQueue

    var peerDiscoveryMode = false

    private var handshakeHelloMessage: HelloMessage? = null

    private var ethInbound = 0
    private var ethOutbound = 0


    lateinit var channel: Channel

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        dev.info("p2p handler added")
        logger.debug("P2P protocol activated")
        mq.activate(ctx)
        startTimers()
    }


    override fun channelRead0(ctx: ChannelHandlerContext, msg: P2pMessage) {
        dev.info("p2p handler channel read0 msg = $msg")
        if (P2pMessageCodes.inRange(msg.code)) logger.trace(
            "P2PHandler invoke: [{}]",
            msg.command
        )
        dev.info("p2p handler invoke ${msg.command}")
        when (msg) {
            is HelloMessage -> {
                mq.receiveMessage(msg)
                setHandshake(msg, ctx)
            }
            is DisconnectMessage -> {
                dev.info("disconnect reason =  ${msg.reason}")
                mq.receiveMessage(msg)
                channel.nodeStatistics.nodeDisconnectedRemote(msg.reason)
                processDisconnect(ctx, msg)
            }
            is PingMessage -> {
                mq.receiveMessage(msg)
                ctx.writeAndFlush(StaticMessages.PONG_MESSAGE)
            }
            is PongMessage -> {
                mq.receiveMessage(msg)
//                channel.nodeStatistics.lastPongReplyTime.set(Util.curTime())
            }
//            -> {
//                msgQueue.receivedMessage(msg)
//                if (peerDiscoveryMode ||
//                    !handshakeHelloMessage.getCapabilities().contains(Capability.ETH)
//                ) {
//                    disconnect(ReasonCode.REQUESTED)
//                    killTimers()
//                    ctx.close().sync()
//                    ctx.disconnect().sync()
//                }
//            }
            else -> ctx.fireChannelRead(msg)
        }
    }

    private fun disconnect(reasonCode: ReasonCode) {
        mq.sendMessage(DisconnectMessage(reasonCode))
        channel.nodeStatistics.nodeDisconnectedLocal(reasonCode)
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.debug("channel inactive: ", ctx.toString())
        killTimers()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable?) {
        logger.warn("P2p handling failed", cause)
        ctx.close()
        killTimers()
    }

    private fun processDisconnect(ctx: ChannelHandlerContext, msg: DisconnectMessage) {
//        if (logger.isInfoEnabled && msg.reason === ReasonCode.USELESS_PEER) {
//            if (channel.nodeStatistics.ethInbound.get() - ethInbound > 1 ||
//                channel.getNodeStatistics().ethOutbound.get() - ethOutbound > 1
//            ) {
//
//                // it means that we've been disconnected
//                // after some incorrect action from our peer
//                // need to log this moment
//                logger.debug("From: \t{}\t [DISCONNECT reason=BAD_PEER_ACTION]", channel)
//            }
//        }
        ctx.close()
        killTimers()
    }

    private fun sendGetPeers() {
//        msgQueue.sendMessage(StaticMessages.GET_PEERS_MESSAGE)
    }


    fun setHandshake(msg: HelloMessage, ctx: ChannelHandlerContext) {
//        channel.nodeStatistics.setClientId(msg.getClientId())
//        channel.getNodeStatistics().capabilities.clear()
//        channel.getNodeStatistics().capabilities.addAll(msg.getCapabilities())
//        ethInbound = channel.getNodeStatistics().ethInbound.get()
//        ethOutbound = channel.getNodeStatistics().ethOutbound.get()
        handshakeHelloMessage = msg
        val capInCommon: List<Capability> = getSupportedCapabilities(msg)
        channel.capabilities = capInCommon
        for (capability in capInCommon) {
            if (capability.name == Capability.ETH) {

                // Activate EthHandler for this peer
                channel.activateEth(ctx, fromCode(capability.version))
            }
//            else if (capability.getName().equals(Capability.SHH) &&
//                capability.getVersion() === ShhHandler.VERSION
//            ) {
//
//                // Activate ShhHandler for this peer
//                channel.activateShh(ctx)
//            } else if (capability.getName().equals(Capability.BZZ) &&
//                capability.getVersion() === BzzHandler.VERSION
//            ) {
//
//                // Activate ShhHandler for this peer
////                channel.activateBzz(ctx)
//            }
        }
    }

    /**
     * submit transaction to the network
     *
     * @param tx - fresh transaction object
     */
//    fun sendTransaction(tx: Transaction?) {
//        val msg = TransactionsMessage(tx)
//        msgQueue.sendMessage(msg)
//    }

//    fun sendNewBlock(block: Block) {
//        val msg = NewBlockMessage(block, block.getDifficulty())
//        msgQueue.sendMessage(msg)
//    }

//    fun sendDisconnect() {
//        msgQueue.disconnect()
//    }

    private fun startTimers() {
        // sample for pinging in background
        GlobalScope.launch {
            for (t in pingTicker) {
                try {
                    mq.sendMessage(PING_MESSAGE)
                } catch (t: Throwable) {
                    logger.error("Unhandled exception", t)
                }
            }
        }
    }

    fun killTimers() {
        pingTicker.cancel()
//        mq.close()
    }

    // intersection of capability, downside capability version
    private fun getSupportedCapabilities(hello: HelloMessage): List<Capability> {
        val configCaps: List<Capability> = staticMessages.configCapabilities
        val supported: MutableList<Capability> = mutableListOf()
        val eths: MutableList<Capability> = mutableListOf()
        for (cap in hello.capabilities) {
            if (configCaps.contains(cap)) {
                if (cap.isEth) {
                    eths.add(cap)
                } else {
                    supported.add(cap)
                }
            }
        }
        if (eths.isEmpty()) {
            return supported
        }

        // we need to pick up
        // the most recent Eth version
        var highest: Capability? = null
        for (eth in eths) {
            if (highest == null || highest.version < eth.version) {
                highest = eth
            }
        }
        highest?.let { supported.add(it) }
        return supported
    }
}