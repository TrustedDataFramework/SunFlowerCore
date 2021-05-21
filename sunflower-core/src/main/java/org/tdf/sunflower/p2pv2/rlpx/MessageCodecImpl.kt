package org.tdf.sunflower.p2pv2.rlpx

import com.google.common.io.ByteStreams
import io.netty.channel.ChannelHandlerContext
import org.apache.commons.collections4.map.LRUMap
import org.springframework.stereotype.Component
import org.tdf.common.util.ByteUtil
import org.tdf.common.util.ByteUtil.toHexString
import org.tdf.rlpstream.Rlp
import org.tdf.sunflower.AppConfig
import org.tdf.sunflower.p2pv2.Loggers
import org.tdf.sunflower.p2pv2.MessageCode
import org.tdf.sunflower.p2pv2.client.Capability
import org.tdf.sunflower.p2pv2.eth.EthVersion
import org.tdf.sunflower.p2pv2.message.Message
import org.tdf.sunflower.p2pv2.message.ReasonCode
import org.tdf.sunflower.p2pv2.server.Channel
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

@Component
class MessageCodecImpl(val cfg: AppConfig) : MessageCodec(), Loggers{
    private var maxFramePayloadSize = NO_FRAMING

    private var _channel: Channel? = null

    override var channel: Channel
        get() = _channel!!
        set(value) {
            _channel = value
        }

    override fun initMessageCodecs(caps: List<Capability>) {

    }


    var ethVersion: EthVersion = EthVersion.V62

    private var supportChunkedFrames = false
    private val incompleteFrames: MutableMap<Int, Pair<MutableList<Frame>, AtomicInteger>> = LRUMap(16)

    // LRU avoids OOM on invalid peers
    var contextIdCounter = AtomicInteger(1)


    init{
        maxFramePayloadSize = cfg.rlpxMaxFrameSize
    }

    override fun decode(ctx: ChannelHandlerContext, msg: Frame, out: MutableList<Any>) {
        val completeFrame: Frame? = null
        if (msg.chunked) {
            if (!supportChunkedFrames && msg.totalFrameSize > 0) {
                throw RuntimeException("Framing is not supported in this configuration.")
            }
            var frameParts = incompleteFrames[msg.contextId]
            if (frameParts == null) {
                if (msg.totalFrameSize < 0) {
//                    loggerNet.warn("No initial frame received for context-id: " + frame.contextId + ". Discarding this frame as invalid.");
                    // TODO: refactor this logic (Cpp sends non-chunked frames with context-id)
                    val message = decodeMessage(ctx, listOf(msg)) ?: return
                    out.add(message)
                    return
                } else {

                    frameParts = Pair(ArrayList(), AtomicInteger(0))
                    incompleteFrames[msg.contextId] = frameParts
                }
            } else {
                if (msg.totalFrameSize >= 0) {
                    net.warn("Non-initial chunked frame shouldn't contain totalFrameSize field (context-id: " + msg.contextId.toString() + ", totalFrameSize: " + msg.totalFrameSize.toString() + "). Discarding this frame and all previous.")
                    incompleteFrames.remove(msg.contextId)
                    return
                }
            }
            frameParts.first.add(msg)
            val curSize = frameParts.second.addAndGet(msg.size)
            if (wire.isDebugEnabled) wire.debug(
                "Recv: Chunked ($curSize of " + frameParts.first[0].totalFrameSize + ") [size: " + msg.size + "]"
            )
            if (curSize > frameParts.first[0].totalFrameSize) {
                net.warn(
                    "The total frame chunks size ($curSize) is greater than expected (" + frameParts.first[0].totalFrameSize + "). Discarding the frame."
                )
                incompleteFrames.remove(msg.contextId)
                return
            }
            if (curSize == frameParts.first[0].totalFrameSize) {
                val message = decodeMessage(ctx, frameParts.first)
                incompleteFrames.remove(msg.contextId)
                message?.let { out.add(it) }
            }
        } else {
            val message = decodeMessage(ctx, listOf(msg))
            message?.let { out.add(it) }
        }
    }


    private fun decodeMessage(ctx: ChannelHandlerContext, frames: List<Frame>): Message? {
        val frameType: Int = frames[0].type
        val payload = ByteArray(if (frames.size == 1) frames[0].size else frames[0].totalFrameSize)
        var pos = 0
        for (frame in frames) {
            pos += ByteStreams.read(frame.stream, payload, pos, frame.size)
        }
        if (wire.isDebugEnabled)
            wire.debug("Recv: Encoded: {} [{}]", frameType, ByteUtil.toHexString(payload))
        val msg: Message
        try {
            msg = createMessage(frameType.toByte(), payload)
            if (net.isDebugEnabled)
                net.debug("From: {}    Recv:  {}", channel, msg.toString())
        } catch (ex: Exception) {
            net.debug(String.format("Incorrectly encoded message from: \t%s, dropping peer", channel), ex)
            channel.disconnect(ReasonCode.BAD_PROTOCOL)
            return null
        }
        channel.nodeStatistics.rlpxInMessages.add()
        return msg
    }


    override fun encode(ctx: ChannelHandlerContext, msg: Message, out: MutableList<Any>) {
        val output = String.format("To: \t%s \tSend: \t%s", ctx.channel().remoteAddress(), msg)
        if (net.isDebugEnabled)
            net.debug("To:   {}    Send:  {}", channel, msg)
        val encoded: ByteArray = Rlp.encode(msg)
        if (wire.isDebugEnabled) wire.debug(
            "Send: Encoded: {} [{}]",
            getCode(msg.command),
            toHexString(encoded)
        )
        val frames = splitMessageToFrames(msg)
        out.addAll(frames)
        channel.nodeStatistics.rlpxOutMessages.add()
    }

    private fun splitMessageToFrames(msg: Message): List<Frame> {
        val code = getCode(msg.command)
        val ret: MutableList<Frame> = ArrayList()
        val bytes: ByteArray = Rlp.encode(msg)
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
            val contextId = contextIdCounter.getAndIncrement()
            ret[0].totalFrameSize = bytes.size
            wire.debug("Message (size " + bytes.size + ") split to " + ret.size + " frames. Context-id: " + contextId)
            for (frame in ret) {
                frame.contextId = contextId
            }
        }
        return ret
    }

    fun setSupportChunkedFrames(supportChunkedFrames: Boolean) {
        this.supportChunkedFrames = supportChunkedFrames
        if (!supportChunkedFrames) {
            maxFramePayloadSize = NO_FRAMING
        }
    }

    /* TODO: this dirty hack is here cause we need to use message
           TODO: adaptive id on high message abstraction level,
           TODO: need a solution here*/
    private fun getCode(msgCommand: MessageCode): Int {
        return 0
    }

    private fun createMessage(code: Byte, payload: ByteArray): Message {
        throw IllegalArgumentException("No such message: " + code + " [" + toHexString(payload) + "]")
    }


    companion object {
        const val NO_FRAMING = Int.MAX_VALUE shr 1
    }
}