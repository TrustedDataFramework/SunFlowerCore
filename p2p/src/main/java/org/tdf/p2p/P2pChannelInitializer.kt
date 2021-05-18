package org.tdf.p2p

import io.netty.channel.*
import io.netty.channel.socket.nio.NioSocketChannel
import org.slf4j.LoggerFactory

class P2pChannelInitializer(private val remoteId: String?) :
    ChannelInitializer<NioSocketChannel>() {

//    private val ctx: org.springframework.context.ApplicationContext? = null

//    var channelManager: ChannelManager? = null

//    var nodeManager: NodeManager? = null

    private var peerDiscoveryMode = false


    override fun initChannel(ch: NioSocketChannel) {
        try {
            if (!peerDiscoveryMode) {
                logger.debug("Open {} connection, channel: {}", if (isInbound) "inbound" else "outbound", ch.toString())
            }
            if (notEligibleForIncomingConnection(ch)) {
                ch.disconnect()
                return
            }

//            val channel: Channel = ctx.getBean<Channel>(Channel::class.java)
//            channel.setInetSocketAddress(ch.remoteAddress())
//            channel.init(ch.pipeline(), remoteId, peerDiscoveryMode, channelManager)
//            if (!peerDiscoveryMode) {
//                channelManager.add(channel)
//            }

            // limit the size of receiving buffer to 1024
            ch.config().recvByteBufAllocator = FixedRecvByteBufAllocator(256 * 1024)
            ch.config().setOption(ChannelOption.SO_RCVBUF, 256 * 1024)
            ch.config().setOption(ChannelOption.SO_BACKLOG, 1024)

            // be aware of channel closing
//            ch.closeFuture().addListener(ChannelFutureListener { future: ChannelFuture? ->
//                if (!peerDiscoveryMode) {
//                    channelManager.notifyDisconnect(channel)
//                }
//            })
        } catch (e: Exception) {
            logger.error("Unexpected error: ", e)
        }
    }

    /**
     * Tests incoming connection channel for usual abuse/attack vectors
     * @param ch    Channel
     * @return true if we should refuse this connection, otherwise false
     */
    private fun notEligibleForIncomingConnection(ch: NioSocketChannel): Boolean {
        if (!isInbound) return false
        // For incoming connection drop if..

        // Bad remote address
        if (ch.remoteAddress() == null) {
            logger.debug("Drop connection - bad remote address, channel: {}", ch.toString())
            return true
        }

        // Drop if we have long waiting queue already
//        if (!channelManager.acceptingNewPeers()) {
//            logger.debug("Drop connection - many new peers are not processed, channel: {}", ch.toString())
//            return true
//        }

        // Refuse connections from ips that are already in connection queue
        // Local and private network addresses are still welcome!
//        if (!ch.remoteAddress().address.isLoopbackAddress &&
//            !ch.remoteAddress().address.isSiteLocalAddress &&
//            channelManager.isAddressInQueue(ch.remoteAddress().address)
//        ) {
//            logger.debug("Drop connection - already processing connection from this host, channel: {}", ch.toString())
//            return true
//        }

        // Avoid too frequent connection attempts
//        if (channelManager.isRecentlyDisconnected(ch.remoteAddress().address)) {
//            logger.debug("Drop connection - the same IP was disconnected recently, channel: {}", ch.toString())
//            return true
//        }

        // Drop bad peers before creating channel
//        if (nodeManager.isReputationPenalized(ch.remoteAddress())) {
//            logger.debug("Drop connection - bad peer, channel: {}", ch.toString())
//            return true
//        }

        return false
    }

    private val isInbound: Boolean
        private get() = remoteId == null || remoteId.isEmpty()

    fun setPeerDiscoveryMode(peerDiscoveryMode: Boolean) {
        this.peerDiscoveryMode = peerDiscoveryMode
    }

    companion object {
        private val logger = LoggerFactory.getLogger("net")
    }
}
