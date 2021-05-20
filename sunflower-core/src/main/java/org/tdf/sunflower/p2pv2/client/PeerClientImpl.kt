package org.tdf.sunflower.p2pv2.client

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelOption
import io.netty.channel.DefaultMessageSizeEstimator
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.tdf.sunflower.AppConfig
import org.tdf.sunflower.p2pv2.Loggers
import org.tdf.sunflower.p2pv2.server.PeerChannelInitializer
import java.io.IOException
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicLong

@Component
class PeerClientImpl @Autowired constructor(val ctx: ApplicationContext, val cfg: AppConfig) : PeerClient, Loggers {
    private val threadCnt = AtomicLong(0L)

    private val threadFactory = ThreadFactory {
        Thread(it, "PeerClientWorker-${threadCnt.getAndIncrement()}")
    }

    private val workerGroup = NioEventLoopGroup(0, threadFactory)

    override fun connect(host: String, port: Int, remoteId: String, discoveryMode: Boolean) {
        try {
            val f = connectAsync(host, port, remoteId, discoveryMode)
            f.sync()

            // Wait until the connection is closed.
            f.channel().closeFuture().sync()
            net.debug("Connection is closed")
        } catch (e: Exception) {
            if (discoveryMode) {
                net.trace("Exception:", e)
            } else {
                if (e is IOException) {
                    net.info("PeerClient: Can't connect to $host:$port ( ${e.message} )")
                    net.debug("PeerClient.connect($host:$port) exception:", e)
                } else {
                    net.error("Exception:", e)
                }
            }
        }
    }

    override fun connectAsync(host: String, port: Int, remoteId: String, discoveryMode: Boolean): ChannelFuture {
        net.trace("Connecting to: $host:$port")

        val initializer = ctx.getBean(PeerChannelInitializer::class.java)
        initializer.peerDiscoveryMode = discoveryMode

        val b = Bootstrap()
        b.group(workerGroup)
        b.channel(NioSocketChannel::class.java)
        b.option(ChannelOption.SO_KEEPALIVE, true)
        b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT)
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, cfg.peerConnectionTimeout)
        b.remoteAddress(host, port)
        b.handler(initializer)

        return b.connect()
    }

    override fun close() {
        net.info("Shutdown peerClient")
        workerGroup.shutdownGracefully()
        workerGroup.terminationFuture().syncUninterruptibly()
    }
}