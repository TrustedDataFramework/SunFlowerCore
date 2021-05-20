package org.tdf.sunflower.p2pv2.rlpx.discover

import org.tdf.sunflower.p2pv2.message.ReasonCode

interface NodeStatistics {
    fun nodeDisconnectedLocal(reason: ReasonCode)
}