package org.tdf.sunflower.p2pv2

import io.netty.channel.ChannelHandlerContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.TickerMode
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.tdf.sunflower.p2pv2.message.Message
import org.tdf.sunflower.p2pv2.p2p.DisconnectMessage
import org.tdf.sunflower.p2pv2.server.Channel
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

@Component @Scope("prototype") class MessageQueueImpl: MessageQueue
{
    val NO_FRAMING = Int.MAX_VALUE shr 1

    private val requestQueue: Queue<MessageRoundtrip> = ConcurrentLinkedQueue()
    private val respondQueue: Queue<MessageRoundtrip> = ConcurrentLinkedQueue()
    private var ctx: ChannelHandlerContext? = null
    private var timerTask: ReceiveChannel<Unit>? = null

    private var _channel: Channel? = null

    override var maxFramePayloadSize: Int = NO_FRAMING

    var _supportChunkedFrames: Boolean = false


    override var supportChunkedFrames: Boolean
        get() = _supportChunkedFrames
        set(v) {
            _supportChunkedFrames = v
            if(!_supportChunkedFrames)
                maxFramePayloadSize = NO_FRAMING
        }


    override var channel: Channel
        get() = _channel!!
        set(v) { _channel = v }

    override fun sendMessage(msg: Message) {
        TODO("Not yet implemented")
    }

    override fun disconnect(msg: DisconnectMessage) {
        TODO("Not yet implemented")
    }

    override fun activate(ctx: ChannelHandlerContext) {
        this.ctx = ctx
        timerTask = ticker(10, 10, mode = TickerMode.FIXED_DELAY)
        GlobalScope.launch {
            for(t in timerTask!!) {
                try {

                } catch (t: Throwable) {
                    log.error("Unhandled exception", t)
                }
            }
        }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger("mq")
    }
}