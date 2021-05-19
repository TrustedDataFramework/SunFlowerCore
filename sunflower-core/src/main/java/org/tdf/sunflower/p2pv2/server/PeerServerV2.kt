package org.tdf.sunflower.p2pv2.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LoggingHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.tdf.common.util.ByteUtil.toHexString
import org.tdf.sunflower.AppConfig

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
    ){
    companion object {
        val log: Logger = LoggerFactory.getLogger("net")
    }

//    private var config: SystemProperties? = null

//    private var ctx: org.springframework.context.ApplicationContext? = null

//    private var ethereumListener: EthereumListener? = null

    var channelInitializer: ChannelInitializerImpl? = null

    private var listening = false

    var bossGroup: EventLoopGroup? = null
    var workerGroup: EventLoopGroup? = null
    var channelFuture: ChannelFuture? = null

    // this method will blocking current thread
    // called by channel manager
    fun start(port: Int) {
        bossGroup = NioEventLoopGroup(1)
        workerGroup = NioEventLoopGroup()
        channelInitializer = ctx.getBean(ChannelInitializerImpl::class.java)

        log.trace("Listening on port $port")
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
            log.info("Listening for incoming connections, port: [{}] ", port)
            // print node id
            log.info("NodeId: [{}] ", toHexString(cfg.nodeId))

            log.info("wait for channel future sync")
            channelFuture = b.bind(port).sync()
            log.info("channel future sync completed")

            listening = true
            // Wait until the connection is closed.
            log.info("blocking until channel closed")
            channelFuture!!.channel().closeFuture().sync()
            log.debug("Connection is closed")
        } catch (e: Exception) {
            log.error("Peer server error: {} ({})", e.message, e.javaClass.name)
            throw Error("Server Disconnected")
        } finally {
            workerGroup?.shutdownGracefully()
            bossGroup?.shutdownGracefully()
            listening = false
        }
    }

    fun close() {
        if (listening && channelFuture != null && channelFuture!!.channel().isOpen) {
            try {
                log.info("Closing PeerServer...")
                channelFuture!!.channel().close().sync()
                log.info("PeerServer closed.")
            } catch (e: Exception) {
                log.warn("Problems closing server channel", e)
            }
        }
    }

    fun isListening(): Boolean {
        return listening
    }
}

