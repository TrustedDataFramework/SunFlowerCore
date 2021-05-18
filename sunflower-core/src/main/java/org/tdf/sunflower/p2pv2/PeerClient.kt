package org.tdf.sunflower.p2pv2

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelOption
import io.netty.channel.DefaultMessageSizeEstimator
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicLong

class PeerClient {
    private val threadCnt = AtomicLong(0L)
    private val threadFactory = ThreadFactory { Thread(it, "PeerClientWorker-${threadCnt.getAndIncrement()}") }
    val workerGroup = NioEventLoopGroup(0, threadFactory)

    fun connectAsync(host: String, port: Int, remoteId: String, discovery: Boolean): ChannelFuture {
        log.trace("Connecting to: $host:$port")

        val b = Bootstrap()
        b.group(workerGroup)
        b.channel(NioSocketChannel::class.java)
        b.option(ChannelOption.SO_KEEPALIVE, true)
        b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT)
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
        b.remoteAddress(host, port)

        return b.connect()
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger("peerClient")
    }
}