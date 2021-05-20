package org.tdf.sunflower.p2pv2.rlpx

import io.netty.channel.ChannelHandlerContext
import org.springframework.stereotype.Component
import org.tdf.sunflower.p2pv2.client.Capability
import org.tdf.sunflower.p2pv2.message.Message
import org.tdf.sunflower.p2pv2.server.Channel

@Component
class MessageCodecImpl: MessageCodec(){
    private var _channel: Channel? = null
    override fun encode(ctx: ChannelHandlerContext, msg: Message, out: MutableList<Any>) {
        TODO("Not yet implemented")
    }

    override fun decode(ctx: ChannelHandlerContext, msg: Frame, out: MutableList<Any>) {
        TODO("Not yet implemented")
    }

    override var channel: Channel
        get() = _channel!!
        set(v) {
            _channel = v
        }

    override fun initMessageCodecs(caps: List<Capability>) {
        TODO("Not yet implemented")
    }
}