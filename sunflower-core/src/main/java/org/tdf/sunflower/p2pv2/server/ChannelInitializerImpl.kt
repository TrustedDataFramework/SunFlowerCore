package org.tdf.sunflower.p2pv2.server

import io.netty.channel.ChannelOption
import io.netty.channel.FixedRecvByteBufAllocator
import io.netty.channel.socket.nio.NioSocketChannel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.tdf.sunflower.p2pv2.Loggers
import org.tdf.sunflower.p2pv2.rlpx.discover.NodeManager

// TODO: inject Node manager
@Component
@Scope("prototype")
class ChannelInitializerImpl @Autowired constructor(
    val channelManager: ChannelManager,
    private val ctx: ApplicationContext,
    private val nodeManager: NodeManager
) : PeerChannelInitializer(), Loggers {
    // called by netty framework, execute when new connection created
    override fun initChannel(ch: NioSocketChannel) {
        try {
            if (!peerDiscoveryMode) {
                net.debug("Open {} connection, channel: {}", if (inbound) "inbound" else "outbound", ch.toString())
            }
            // validate protocol
            if (notEligibleForIncomingConnection(ch)) {
                ch.disconnect()
                return
            }

            // create channel at application level
            val channel: Channel = ctx.getBean(Channel::class.java)
            channel.inetSocketAddress = ch.remoteAddress()
            channel.init(ch.pipeline(), remoteId, peerDiscoveryMode, channelManager)
            if (!peerDiscoveryMode) {
                channelManager.add(channel)
            }

            // limit the size of receiving buffer to 1024
            ch.config().recvByteBufAllocator = FixedRecvByteBufAllocator(256 * 1024)
            ch.config().setOption(ChannelOption.SO_RCVBUF, 256 * 1024)
            ch.config().setOption(ChannelOption.SO_BACKLOG, 1024)

            // be aware of channel closing
            ch.closeFuture().addListener {
                if (!peerDiscoveryMode) {
                    channelManager.notifyDisconnect(channel)
                }
            }
        } catch (e: Exception) {
            net.error("Unexpected error: ", e)
        }
    }

    /**
     * Tests incoming connection channel for usual abuse/attack vectors
     * @param ch    Channel
     * @return true if we should refuse this connection, otherwise false
     */
    private fun notEligibleForIncomingConnection(ch: NioSocketChannel): Boolean {
        // outbound connection is not eligible
        if (!inbound) return false
        // For incoming connection drop if..

        // Bad remote address
        if (ch.remoteAddress() == null) {
            net.debug(
                "Drop connection - bad remote address, channel: {}",
                ch.toString()
            )
            return true
        }
        // Drop if we have long waiting queue already
        if (!channelManager.acceptingNewPeers) {
            net.debug(
                "Drop connection - many new peers are not processed, channel: {}",
                ch.toString()
            )
            return true
        }
        // Refuse connections from ips that are already in connection queue
        // Local and private network addresses are still welcome!
        if (!ch.remoteAddress().address.isLoopbackAddress &&
            !ch.remoteAddress().address.isSiteLocalAddress &&
            channelManager.isAddressInQueue(ch.remoteAddress().address)
        ) {
            net.debug(
                "Drop connection - already processing connection from this host, channel: {}",
                ch.toString()
            )
            return true
        }

        // Avoid too frequent connection attempts
        if (channelManager.isRecentlyDisconnected(ch.remoteAddress().address)) {
            net.debug(
                "Drop connection - the same IP was disconnected recently, channel: {}",
                ch.toString()
            )
            return true
        }
        // Drop bad peers before creating channel
        if (nodeManager.isReputationPenalized(ch.remoteAddress())) {
            net.debug(
                "Drop connection - bad peer, channel: {}",
                ch.toString()
            )
            return true
        }
        return false
    }


    private val inbound: Boolean
        get() = remoteId.isEmpty()

}
