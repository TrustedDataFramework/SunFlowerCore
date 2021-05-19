package org.tdf.sunflower.p2pv2.server

import org.apache.commons.collections4.map.LRUMap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.AppConfig
import java.net.InetAddress
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList


// TODO: add node into node manager
@Component
class ChannelManager @Autowired constructor
    (val server: PeerServerV2, val cfg: AppConfig)
{
    companion object {
        // If the inbound peer connection was dropped by us with a reason message
        // then we ban that peer IP on any connections for some time to protect from
        // too active peers
        const val INBOUND_CONNECTION_BAN_TIMEOUT = 120 * 1000
        const val MAX_NEW_PEERS = 128;
    }

    private val maxActivePeers = cfg.maxActivePeers;
    private val activePeers: MutableMap<HexBytes, Channel> = ConcurrentHashMap()
    private val newPeers: MutableList<Channel> = CopyOnWriteArrayList()
    private val recentlyDisconnected: MutableMap<InetAddress, Date> = Collections.synchronizedMap(LRUMap(500))

    // start the peer server
    init {
        if(cfg.listenPort > 0) {
            Thread({
                this.server.start(cfg.listenPort)
            }, "PeerServerThread").start()
        }
    }

    fun add(peer: Channel) {
        newPeers.add(peer)
    }

    /**
     * Checks whether newPeers is not full
     * newPeers are used to fill up active peers
     * @return True if there are free slots for new peers
     */
    fun acceptingNewPeers(): Boolean {
        return true
    }

    /**
     * Whether peer with the same ip is in newPeers, waiting for processing
     * @param peerAddr      Peer address
     * @return true if we already have connection from this address, otherwise false
     */
    fun isAddressInQueue(peerAddr: InetAddress): Boolean {
        for (peer in newPeers) {
            if (peer.inetSocketAddress != null &&
                peer.inetSocketAddress?.address?.hostAddress.equals(peerAddr.hostAddress)
            ) {
                return true
            }
        }
        return false
    }

    fun isRecentlyDisconnected(peerAddr: InetAddress): Boolean {
        val disconnectTime: Date? = recentlyDisconnected[peerAddr]
        return if (disconnectTime != null &&
            System.currentTimeMillis() - disconnectTime.time < INBOUND_CONNECTION_BAN_TIMEOUT
        ) {
            true
        } else {
            recentlyDisconnected.remove(peerAddr)
            false
        }
    }

}