package org.tdf.sunflower.p2pv2.rlpx

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageCodec
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.tdf.sunflower.p2pv2.message.Message

@Component
@Scope("prototype")
class MessageCodec: MessageToMessageCodec<Frame, Message>() {
    override fun encode(ctx: ChannelHandlerContext?, msg: Message?, out: MutableList<Any>?) {
        TODO("Not yet implemented")
    }

    override fun decode(ctx: ChannelHandlerContext?, msg: Frame?, out: MutableList<Any>?) {
        TODO("Not yet implemented")
    }


    companion object {
        val loggerWire = LoggerFactory.getLogger("wire")
        val loggerNet = LoggerFactory.getLogger("net")
    }


}