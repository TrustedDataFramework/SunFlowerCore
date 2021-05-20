package org.tdf.sunflower.p2pv2.rlpx

import com.google.common.io.ByteStreams
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageCodec
import org.apache.commons.collections4.map.LRUMap
import org.apache.commons.lang3.tuple.Pair
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.tdf.common.util.ByteUtil.toHexString
import org.tdf.rlpstream.Rlp
import org.tdf.sunflower.p2pv2.P2pMessageCodes
import org.tdf.sunflower.p2pv2.eth.message.EthMessageCodes
import org.tdf.sunflower.p2pv2.message.Message
import org.tdf.sunflower.p2pv2.server.Channel
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

@Component
@Scope("prototype")
class MessageCodec: MessageToMessageCodec<Frame, Message>() {
    private var supportChunkedFrames = false
    private val incompleteFrames: MutableMap<Int, Pair<out MutableList<Frame>, AtomicInteger>> = LRUMap(16)
    var channel: Channel? = null

    fun getSupportChunkedFrames(): Boolean{
        return supportChunkedFrames
    }

    override fun decode(ctx: ChannelHandlerContext, frame: Frame, out: MutableList<Any>) {
        val completeFrame: Frame? = null
        if (frame.chunked) {
            if (!supportChunkedFrames && frame.totalFrameSize > 0) {
                throw RuntimeException("Framing is not supported in this configuration.")
            }
            var frameParts: Pair<out MutableList<Frame>, AtomicInteger>? = incompleteFrames[frame.contextId]
            if (frameParts == null) {
                if (frame.totalFrameSize < 0) {
//                    loggerNet.warn("No initial frame received for context-id: " + frame.contextId + ". Discarding this frame as invalid.");
                    // TODO: refactor this logic (Cpp sends non-chunked frames with context-id)
                    val message = decodeMessage(ctx, listOf(frame)) ?: return
                    out.add(message)
                    return
                } else {
                    frameParts = Pair.of(ArrayList(), AtomicInteger(0))
                    incompleteFrames[frame.contextId] = frameParts
                }
            } else {
                if (frame.totalFrameSize >= 0) {
                    loggerNet.warn("Non-initial chunked frame shouldn't contain totalFrameSize field (context-id: " + frame.contextId.toString() + ", totalFrameSize: " + frame.totalFrameSize.toString() + "). Discarding this frame and all previous.")
                    incompleteFrames.remove(frame.contextId)
                    return
                }
            }
            frameParts.left!!.add(frame)
            val curSize = frameParts.right.addAndGet(frame.size)
            if (loggerWire.isDebugEnabled) loggerWire.debug(
                "Recv: Chunked ($curSize of " + frameParts.left!![0]!!.totalFrameSize + ") [size: " + frame.getSize() + "]"
            )
            if (curSize > frameParts.left!![0]!!.totalFrameSize) {
                loggerNet.warn(
                    "The total frame chunks size ($curSize) is greater than expected (" + frameParts.left!![0]!!.totalFrameSize + "). Discarding the frame."
                )
                incompleteFrames.remove(frame.contextId)
                return
            }
            if (curSize == frameParts.left!![0]!!.totalFrameSize) {
                val message = decodeMessage(ctx, frameParts.left)
                incompleteFrames.remove(frame.contextId)
                out.add(message)
            }
        } else {
            val message = decodeMessage(ctx, listOf(frame))
            out.add(message)
        }
    }

    private fun decodeMessage(ctx: ChannelHandlerContext, frames: List<Frame>): Message {
        val frameType = frames[0].type
        val payload = ByteArray(
            if (frames.size == 1) frames[0].size else frames[0].totalFrameSize
        )
        var pos = 0
        for (frame in frames) {
            pos += ByteStreams.read(frame.stream, payload, pos, frame.size)
        }
        if (loggerWire.isDebugEnabled) loggerWire.debug("Recv: Encoded: {} [{}]", frameType, toHexString(payload))
        val msg: Message
        try {
            msg = createMessage(frameType.toByte(), payload)
            if (loggerNet.isDebugEnabled) loggerNet.debug("From: {}    Recv:  {}", channel, msg.toString())
        } catch (ex: Exception) {
            loggerNet.debug(String.format("Incorrectly encoded message from: \t%s, dropping peer", channel), ex)
//            channel.disconnect(ReasonCode.BAD_PROTOCOL)
            throw ex
        }
//        channel.getNodeStatistics().rlpxInMessages.add()
        return msg
    }

    override fun encode(ctx: ChannelHandlerContext, msg: Message, out: MutableList<Any>) {
        val output = String.format("To: \t%s \tSend: \t%s", ctx.channel().remoteAddress(), msg)
        loggerWire.trace(output)
        if (loggerNet.isDebugEnabled) loggerNet.debug("To:   {}    Send:  {}", channel, msg)
        val encoded: ByteArray = Rlp.encode(msg)
        if (loggerWire.isDebugEnabled) loggerWire.debug(
            "Send: Encoded: {} [{}]",
            msg.command,
            toHexString(encoded)
        )
        val frames = splitMessageToFrames(msg)
        out.addAll(frames)
        channel.getNodeStatistics().rlpxOutMessages.add()
    }

    private fun splitMessageToFrames(msg: Message): List<Frame> {
        val code = getCode(msg.getCommand())
        val ret: MutableList<Frame> = ArrayList()
        val bytes: ByteArray = msg.getEncoded()
        var curPos = 0
        while (curPos < bytes.size) {
            val newPos = Math.min(curPos + maxFramePayloadSize, bytes.size)
            val frameBytes =
                if (curPos == 0 && newPos == bytes.size) bytes else Arrays.copyOfRange(bytes, curPos, newPos)
            ret.add(Frame(code.toInt(), frameBytes))
            curPos = newPos
        }
        if (ret.size > 1) {
            // frame has been split
            val contextId: Int = contextIdCounter.getAndIncrement()
            ret[0].totalFrameSize = bytes.size
            loggerWire.debug("Message (size " + bytes.size + ") split to " + ret.size + " frames. Context-id: " + contextId)
            for (frame in ret) {
                frame.contextId = contextId
            }
        }
        return ret
    }

    fun setSupportChunkedFrames(supportChunkedFrames: Boolean) {
        this.supportChunkedFrames = supportChunkedFrames
        if (!supportChunkedFrames) {
            setMaxFramePayloadSize(MessageCodec.NO_FRAMING)
        }
    }

    /* TODO: this dirty hack is here cause we need to use message
           TODO: adaptive id on high message abstraction level,
           TODO: need a solution here*/
    private fun getCode(msgCommand: Enum<*>): Byte {
        var code: Byte = 0
        if (msgCommand is P2pMessageCodes) {
            code = messageCodesResolver.withP2pOffset((msgCommand as P2pMessageCodes).asByte())
        }
        if (msgCommand is EthMessageCodes) {
            code = messageCodesResolver.withEthOffset((msgCommand as EthMessageCodes).asByte())
        }
        if (msgCommand is ShhMessageCodes) {
            code = messageCodesResolver.withShhOffset((msgCommand as ShhMessageCodes).asByte())
        }
        if (msgCommand is BzzMessageCodes) {
            code = messageCodesResolver.withBzzOffset((msgCommand as BzzMessageCodes).asByte())
        }
        return code
    }

    private fun createMessage(code: Byte, payload: ByteArray): Message {
        var resolved: Byte = messageCodesResolver.resolveP2p(code)
        if (p2pMessageFactory != null && P2pMessageCodes.inRange(resolved)) {
            return p2pMessageFactory.create(resolved, payload)
        }
        resolved = messageCodesResolver.resolveEth(code)
        if (ethMessageFactory != null && EthMessageCodes.inRange(resolved, ethVersion)) {
            return ethMessageFactory.create(resolved, payload)
        }
        resolved = messageCodesResolver.resolveShh(code)
        if (shhMessageFactory != null && ShhMessageCodes.inRange(resolved)) {
            return shhMessageFactory.create(resolved, payload)
        }
        resolved = messageCodesResolver.resolveBzz(code)
        if (bzzMessageFactory != null && BzzMessageCodes.inRange(resolved)) {
            return bzzMessageFactory.create(resolved, payload)
        }
        throw IllegalArgumentException("No such message: " + code + " [" + toHexString(payload) + "]")
    }

    companion object {
        val loggerWire = LoggerFactory.getLogger("wire")
        val loggerNet = LoggerFactory.getLogger("net")
    }


}