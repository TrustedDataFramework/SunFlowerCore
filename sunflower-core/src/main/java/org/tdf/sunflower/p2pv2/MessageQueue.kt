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
import org.tdf.sunflower.p2pv2.server.ChannelImpl
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

@Component @Scope("prototype") class MessageQueue
{
    private val requestQueue: Queue<MessageRoundtrip> = ConcurrentLinkedQueue()
    private val respondQueue: Queue<MessageRoundtrip> = ConcurrentLinkedQueue()
    private var ctx: ChannelHandlerContext? = null
    private var timerTask: ReceiveChannel<Unit>? = null
    var channel: ChannelImpl? = null

    fun activate(ctx: ChannelHandlerContext) {
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