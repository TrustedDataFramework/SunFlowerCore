package org.tdf.sunflower.p2pv2

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.channel.socket.DatagramPacket
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.TickerMode
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.tdf.common.util.CommonUtils.sizeToStr
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.annotation.PreDestroy

@Component
class WireTrafficStats() : Runnable {
    private val logger = LoggerFactory.getLogger("net")

    private val ticker = ticker(
        TimeUnit.SECONDS.toMillis(10L),
        TimeUnit.SECONDS.toMillis(10L),
        mode = TickerMode.FIXED_DELAY
    )
    val tcp = TrafficStatHandler()
    val udp = TrafficStatHandler()

    init {
        GlobalScope.launch {
            for (x in ticker) {
                this@WireTrafficStats.run()
            }
        }
    }

    override fun run() {
        logger.info("TCP: " + tcp.stats())
        logger.info("UDP: " + udp.stats())
    }

    @PreDestroy
    fun close() {
        ticker.cancel()
    }

    @ChannelHandler.Sharable
    class TrafficStatHandler : ChannelDuplexHandler() {
        var outSizeTot: Long = 0
        var inSizeTot: Long = 0
        var outSize = AtomicLong()
        var inSize = AtomicLong()
        var outPackets = AtomicLong()
        var inPackets = AtomicLong()
        var lastTime = System.currentTimeMillis()
        fun stats(): String {
            val out = outSize.getAndSet(0)
            val outPac = outPackets.getAndSet(0)
            val `in` = inSize.getAndSet(0)
            val inPac = inPackets.getAndSet(0)
            outSizeTot += out
            inSizeTot += `in`
            val curTime = System.currentTimeMillis()
            val d = curTime - lastTime
            val outSpeed = out * 1000 / d
            val inSpeed = `in` * 1000 / d
            lastTime = curTime
            return "Speed in/out " + sizeToStr(inSpeed).toString() + " / " + sizeToStr(outSpeed).toString() +
                    "(sec), packets in/out " + inPac.toString() + "/" + outPac.toString() +
                    ", total in/out: " + sizeToStr(inSizeTot).toString() + " / " + sizeToStr(outSizeTot)
        }

        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            inPackets.incrementAndGet()
            if (msg is ByteBuf) {
                inSize.addAndGet(msg.readableBytes().toLong())
            } else if (msg is DatagramPacket) {
                inSize.addAndGet(msg.content().readableBytes().toLong())
            }
            super.channelRead(ctx, msg)
        }

        override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
            outPackets.incrementAndGet()
            if (msg is ByteBuf) {
                outSize.addAndGet(msg.readableBytes().toLong())
            } else if (msg is DatagramPacket) {
                outSize.addAndGet(msg.content().readableBytes().toLong())
            }
            super.write(ctx, msg, promise)
        }
    }
}