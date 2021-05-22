package org.tdf.sunflower.p2pv2.rlpx

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import org.tdf.sunflower.p2pv2.server.Channel

abstract class HandshakeHandler : ByteToMessageDecoder() {
    abstract override fun channelActive(ctx: ChannelHandlerContext)
    abstract override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: List<Any>)

    // called only when outbound
    abstract fun initiate(ctx: ChannelHandlerContext)
    abstract fun setRemote(remoteId: String, channel: Channel)
    abstract val remoteId: ByteArray
}