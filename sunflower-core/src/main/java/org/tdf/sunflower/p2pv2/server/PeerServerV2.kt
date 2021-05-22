package org.tdf.sunflower.p2pv2.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelOption
import io.netty.channel.DefaultMessageSizeEstimator
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LoggingHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.tdf.common.util.ByteUtil.toHexString
import org.tdf.sunflower.AppConfig
import org.tdf.sunflower.p2pv2.Loggers

/**
 * This class establishes a listener for incoming connections.
 * See [http://netty.io](http://netty.io).
 *
 * @author Roman Mandeleil
 * @since 01.11.2014
 */
@Component
class PeerServerV2 @Autowired constructor(
    private val ctx: ApplicationContext,
    private val cfg: AppConfig
) : Loggers {
    var channelInitializer: ChannelInitializerImpl? = null

    var listening = false
        private set

    private var bossGroup: EventLoopGroup? = null
    private var workerGroup: EventLoopGroup? = null
    private var channelFuture: ChannelFuture? = null

    // this method will blocking current thread
    // called by channel manager
    fun start(port: Int) {
        bossGroup = NioEventLoopGroup(1)
        workerGroup = NioEventLoopGroup()
        channelInitializer = ctx.getBean(ChannelInitializerImpl::class.java)

        net.trace("Listening on port $port")
        try {
            val b = ServerBootstrap()
            b.group(bossGroup, workerGroup)
            b.channel(NioServerSocketChannel::class.java)
            b.option(ChannelOption.SO_KEEPALIVE, true)
            b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT)
            // set peer connection timeout
            b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, cfg.peerConnectionTimeout)

            // serving requests
            b.handler(LoggingHandler())
            b.childHandler(channelInitializer)

            // Start the client.
            net.info("Listening for incoming connections, port: [{}] ", port)
            // print node id
            net.info("NodeId: [{}] ", toHexString(cfg.nodeId))

            net.info("wait for channel future sync")
            channelFuture = b.bind(port).sync()
            net.info("channel future sync completed")

            listening = true
            // Wait until the connection is closed.
            net.info("blocking until channel closed")
            channelFuture!!.channel().closeFuture().sync()
            net.debug("Connection is closed")
        } catch (e: Exception) {
            net.error("Peer server error: {} ({})", e.message, e.javaClass.name)
            throw Error("Server Disconnected")
        } finally {
            workerGroup?.shutdownGracefully()
            bossGroup?.shutdownGracefully()
            listening = false
        }
    }

    fun close() {
        if (listening && (channelFuture?.channel()?.isOpen == true)) {
            try {
                net.info("Closing PeerServer...")
                channelFuture!!.channel().close().sync()
                net.info("PeerServer closed.")
            } catch (e: Exception) {
                net.warn("Problems closing server channel", e)
            }
        }
    }
}

