package org.tdf.sunflower.p2pv2.rlpx

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageCodec
import org.tdf.sunflower.p2pv2.Loggers
import org.tdf.sunflower.p2pv2.message.ReasonCode
import org.tdf.sunflower.p2pv2.server.Channel
import org.xerial.snappy.Snappy
import java.io.IOException

class SnappyCodec(var channel: Channel) : MessageToMessageCodec<Frame, Frame>(), Loggers {

    override fun encode(ctx: ChannelHandlerContext, msg: Frame, out: MutableList<Any>) {

        // stay consistent with decoding party
        if (msg.size > MAX_SIZE) {
            net.info("{}: outgoing frame size exceeds the limit ({} bytes), disconnect", channel, msg.size)
            channel.disconnect(ReasonCode.USELESS_PEER)
            return
        }
        val input = ByteArray(msg.size)
        msg.stream.read(input)
        val compressed = Snappy.rawCompress(input, input.size)
        out.add(Frame(msg.type, compressed))
    }

    override fun decode(ctx: ChannelHandlerContext, msg: Frame, out: MutableList<Any>) {
        val input = ByteArray(msg.size)
        msg.stream.read(input)
        val uncompressedLength = Snappy.uncompressedLength(input).toUInt().toLong()
        if (uncompressedLength > MAX_SIZE) {
            net.info(
                "{}: uncompressed frame size exceeds the limit ({} bytes), drop the peer",
                channel,
                uncompressedLength
            )
            channel.disconnect(ReasonCode.BAD_PROTOCOL)
            return
        }
        val uncompressed = ByteArray(uncompressedLength.toInt())
        try {
            Snappy.rawUncompress(input, 0, input.size, uncompressed, 0)
        } catch (e: IOException) {
            val detailMessage = e.message
            // 5 - error code for framed snappy
            if (detailMessage!!.startsWith("FAILED_TO_UNCOMPRESS") && detailMessage.contains("5")) {
                net.info("{}: Snappy frames are not allowed in DEVp2p protocol, drop the peer", channel)
                channel.disconnect(ReasonCode.BAD_PROTOCOL)
                return
            } else {
                throw e
            }
        }
        out.add(Frame(msg.type, uncompressed))
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        if (channel.discoveryMode) {
            net.trace("SnappyCodec failed: $cause")
        } else {
            if (cause is IOException) {
                net.debug("SnappyCodec failed: " + ctx.channel().remoteAddress() + ": " + cause)
            } else {
                net.warn("SnappyCodec failed: ", cause)
            }
        }
        ctx.close()
    }

    companion object {
        private const val SNAPPY_P2P_VERSION = 5
        private const val MAX_SIZE = 16 * 1024 * 1024 // 16 mb
        fun isSupported(p2pVersion: Int): Boolean {
            return p2pVersion >= SNAPPY_P2P_VERSION
        }
    }
}