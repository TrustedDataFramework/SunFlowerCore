package org.tdf.sunflower.p2pv2.rlpx.discover

import org.tdf.sunflower.p2pv2.Node
import java.net.InetSocketAddress

interface NodeManager {
    fun isReputationPenalized(addr: InetSocketAddress): Boolean {
        return false
    }

    fun getNodeStatistics(node: Node): NodeStatistics
}