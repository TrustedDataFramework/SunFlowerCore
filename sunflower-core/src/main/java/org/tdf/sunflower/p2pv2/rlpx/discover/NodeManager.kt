package org.tdf.sunflower.p2pv2.rlpx.discover

import java.net.InetSocketAddress

interface NodeManager {
    fun isReputationPenalized(addr: InetSocketAddress): Boolean {
        return false
    }
}