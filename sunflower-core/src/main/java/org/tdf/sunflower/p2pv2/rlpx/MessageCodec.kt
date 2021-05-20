package org.tdf.sunflower.p2pv2.rlpx

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageCodec
import org.tdf.sunflower.p2pv2.client.Capability
import org.tdf.sunflower.p2pv2.message.Message
import org.tdf.sunflower.p2pv2.server.Channel

abstract class MessageCodec: MessageToMessageCodec<Frame, Message>(){
    abstract override fun encode(ctx: ChannelHandlerContext, msg: Message, out: MutableList<Any>)

    abstract override fun decode(ctx: ChannelHandlerContext, msg: Frame, out: MutableList<Any>)

    abstract var channel: Channel

    abstract fun initMessageCodecs(caps: List<Capability>)
}