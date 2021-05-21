package org.tdf.sunflower.p2pv2.rlpx.discover

import org.springframework.stereotype.Component
import org.tdf.sunflower.p2pv2.Node

@Component
class NodeManagerImpl : NodeManager {
    override fun getNodeStatistics(node: Node): NodeStatistics {
        return NodeStatisticsImpl()
    }
}