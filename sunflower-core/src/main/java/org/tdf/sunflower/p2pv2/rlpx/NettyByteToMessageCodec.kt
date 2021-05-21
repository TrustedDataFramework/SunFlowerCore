package org.tdf.sunflower.p2pv2.rlpx

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageCodec
import io.netty.handler.codec.ByteToMessageDecoder

/**
 * Since decoder field is not modifiable in the ByteToMessageCodec this class
 * overrides it to set the COMPOSITE_CUMULATOR for ByteToMessageDecoder as it
 * is more effective than the default one.
 */
abstract class NettyByteToMessageCodec<I> : ByteToMessageCodec<I>() {
    private val decoder: ByteToMessageDecoder = object : ByteToMessageDecoder() {
        public override fun decode(ctx: ChannelHandlerContext, `in`: ByteBuf, out: List<Any>) {
            this@NettyByteToMessageCodec.decode(ctx, `in`, out)
        }

        override fun decodeLast(ctx: ChannelHandlerContext, `in`: ByteBuf, out: List<Any>) {
            this@NettyByteToMessageCodec.decodeLast(ctx, `in`, out)
        }

        init {
            setCumulator(COMPOSITE_CUMULATOR)
        }
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        decoder.channelReadComplete(ctx)
        super.channelReadComplete(ctx)
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        decoder.channelInactive(ctx)
        super.channelInactive(ctx)
    }

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        decoder.handlerAdded(ctx)
        super.handlerAdded(ctx)
    }

    override fun handlerRemoved(ctx: ChannelHandlerContext) {
        decoder.handlerRemoved(ctx)
        super.handlerRemoved(ctx)
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        decoder.channelRead(ctx, msg)
    }
}