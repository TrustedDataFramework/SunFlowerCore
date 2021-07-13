package org.tdf.sunflower.p2pv2

import io.netty.channel.ChannelHandlerContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.tdf.sunflower.p2pv2.message.Message
import org.tdf.sunflower.p2pv2.p2p.DisconnectMessage
import org.tdf.sunflower.p2pv2.server.Channel
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

@Component
@Scope("prototype")
class MessageQueueImpl : MessageQueue, Loggers {
    val NO_FRAMING = Int.MAX_VALUE shr 1

    private val requestQueue: Queue<MessageRoundtrip> = ConcurrentLinkedQueue()
    private val respondQueue: Queue<MessageRoundtrip> = ConcurrentLinkedQueue()
    private var ctx: ChannelHandlerContext? = null

    override lateinit var channel: Channel

    override var maxFramePayloadSize: Int = NO_FRAMING

    override var hasPing: Boolean = false
        private set


    override var supportChunkedFrames: Boolean = false
        get() = field
        set(v) {
            field = v
            if (!field)
                maxFramePayloadSize = NO_FRAMING
        }

    override fun sendMessage(msg: Message) {
        if (channel.isDisconnected) {
            net.warn(
                "{}: attempt to send [{}] message after disconnect",
                channel,
                msg.command
            )
            return
        }
        requestQueue.add(MessageRoundtrip(msg))
    }

    override fun disconnect(msg: DisconnectMessage) {
    }

    override fun receiveMessage(msg: Message) {
    }


    override fun activate(ctx: ChannelHandlerContext) {
//        this.ctx = ctx
//        timerTask = ticker(10, 10, mode = TickerMode.FIXED_DELAY)
//        GlobalScope.launch {
//            for (t in timerTask!!) {
//                try {
//                    requestQueue.poll()?.let {
//                        dev.info("send msg to remote ${it.msg}")
//                        ctx.writeAndFlush(it.msg)
//                    }
//                } catch (t: Throwable) {
//                    log.error("Unhandled exception", t)
//                }
//            }
//        }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger("mq")
    }
}