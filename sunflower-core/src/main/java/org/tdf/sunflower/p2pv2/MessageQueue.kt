package org.tdf.sunflower.p2pv2

import io.netty.channel.ChannelHandlerContext
import org.tdf.sunflower.p2pv2.message.Message
import org.tdf.sunflower.p2pv2.message.ReasonCode
import org.tdf.sunflower.p2pv2.p2p.DisconnectMessage
import org.tdf.sunflower.p2pv2.server.Channel

interface MessageQueue {
    fun activate(ctx: ChannelHandlerContext)
    var channel: Channel
    var supportChunkedFrames: Boolean
    var maxFramePayloadSize: Int

    val hasPing: Boolean
    fun sendMessage(msg: Message)

    fun disconnect(reason: ReasonCode = ReasonCode.UNKNOWN) {
        disconnect(DisconnectMessage(reason))
    }

    fun disconnect(msg: DisconnectMessage)

    fun receiveMessage(msg: Message)
}