package org.tdf.sunflower.p2pv2.rlpx.discover

import org.tdf.sunflower.p2pv2.message.ReasonCode

abstract class NodeStatistics {
    abstract fun nodeDisconnectedLocal(reason: ReasonCode)

    val discoverOutPing = StatHandler()
    val discoverInPong = StatHandler()
    val discoverOutPong = StatHandler()
    val discoverInPing = StatHandler()
    val discoverInFind = StatHandler()
    val discoverOutFind = StatHandler()
    val discoverInNeighbours = StatHandler()
    val discoverOutNeighbours = StatHandler()

    val rlpxConnectionAttempts = StatHandler()
    val rlpxAuthMessagesSent = StatHandler()
    val rlpxOutHello = StatHandler()
    val rlpxInHello = StatHandler()
    val rlpxHandshake = StatHandler()
    val rlpxOutMessages = StatHandler()
    val rlpxInMessages = StatHandler()

    var lastDisconnectedTime: Long = 0L
        protected set
    var rlpxLastRemoteDisconnectReason: ReasonCode? = null
        protected set

    open fun nodeDisconnectedRemote(reason: ReasonCode) {
        lastDisconnectedTime = System.currentTimeMillis()
        rlpxLastRemoteDisconnectReason = reason
    }
}