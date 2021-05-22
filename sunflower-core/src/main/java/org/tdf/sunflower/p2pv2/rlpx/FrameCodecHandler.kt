package org.tdf.sunflower.p2pv2.rlpx

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import org.tdf.sunflower.p2pv2.Loggers
import org.tdf.sunflower.p2pv2.server.Channel
import java.io.IOException

class FrameCodecHandler(var frameCodec: FrameCodec, var channel: Channel) : NettyByteToMessageCodec<Frame>(), Loggers {
    override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>) {
        dev.info("frame codec decode")
        if (input.readableBytes() == 0) {
            wire.trace("in.readableBytes() == 0")
            return
        }
        wire.trace("Decoding frame (" + input.readableBytes() + " bytes)")
        val frames = frameCodec.readFrames(input)


        // Check if a full frame was available.  If not, we'll try later when more bytes come in.
        if (frames.isEmpty()) return
        for (i in frames.indices) {
            val (type, size, stream) = frames[i]
            channel.nodeStatistics.rlpxInMessages.add()
        }
        out.addAll(frames)
    }

    override fun encode(ctx: ChannelHandlerContext, frame: Frame, out: ByteBuf) {
        dev.info("frame codec encode")
        frameCodec.writeFrame(frame, out)
        channel.nodeStatistics.rlpxOutMessages.add()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        if (channel.discoveryMode) {
            net.trace("FrameCodec failed: $cause")
        } else {
            if (cause is IOException) {
                net.debug("FrameCodec failed: " + ctx.channel().remoteAddress() + ": " + cause)
            } else {
                net.warn("FrameCodec failed: ", cause)
            }
        }
        ctx.close()
    }
}