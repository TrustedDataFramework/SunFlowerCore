package org.tdf.p2p

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelOption
import io.netty.channel.DefaultMessageSizeEstimator
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LoggingHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Server {
    companion object {
        val log: Logger = LoggerFactory.getLogger("net")
    }

//    private var config: SystemProperties? = null

//    private var ctx: org.springframework.context.ApplicationContext? = null

//    private var ethereumListener: EthereumListener? = null

//    var ethereumChannelInitializer: EthereumChannelInitializer? = null

    private var listening = false

    var bossGroup: EventLoopGroup? = null
    var workerGroup: EventLoopGroup? = null
    var channelFuture: ChannelFuture? = null

    fun start(port: Int) {
        bossGroup = NioEventLoopGroup(1)
        workerGroup = NioEventLoopGroup()
//        ethereumChannelInitializer = ctx.getBean(EthereumChannelInitializer::class.java, "")

        log.trace("Listening on port $port")
        try {
            val b = ServerBootstrap()
            b.group(bossGroup, workerGroup)
            b.channel(NioServerSocketChannel::class.java)
            b.option(ChannelOption.SO_KEEPALIVE, true)
            b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT)

            // set peer connection timeout
            b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)

            // serving requests
            b.handler(LoggingHandler())
            b.childHandler(P2pChannelInitializer("rrr"))

            // Start the client.
            log.info("Listening for incoming connections, port: [{}] ", port)
            // print node id
//            log.info("NodeId: [{}] ", toHexString(config.nodeId()))

            channelFuture = b.bind(port).sync()
            listening = true
            // Wait until the connection is closed.
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