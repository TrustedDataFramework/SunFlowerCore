package org.tdf.sunflower.p2pv2.rlpx

import com.google.common.io.ByteStreams
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.spongycastle.crypto.InvalidCipherTextException
import org.spongycastle.math.ec.ECPoint
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.tdf.common.crypto.ECIESCoder
import org.tdf.common.crypto.ECKey
import org.tdf.common.util.BigEndian
import org.tdf.common.util.ByteUtil
import org.tdf.common.util.HexBytes
import org.tdf.rlpstream.Rlp
import org.tdf.sunflower.AppConfig
import org.tdf.sunflower.p2pv2.P2pMessageCodes
import org.tdf.sunflower.p2pv2.p2p.HelloMessage
import org.tdf.sunflower.p2pv2.server.Channel
import org.tdf.sunflower.p2pv2.server.ChannelImpl
import java.net.InetSocketAddress

/**
 * The Netty handler which manages initial negotiation with peer
 * (when either we initiating connection or remote peer initiates)
 *
 * The initial handshake includes:
 * - first AuthInitiate -> AuthResponse messages when peers exchange with secrets
 * - second P2P Hello messages when P2P protocol and subprotocol capabilities are negotiated
 *
 * After the handshake is done this handler reports secrets and other data to the Channel
 * which installs further handlers depending on the protocol parameters.
 * This handler is finally removed from the pipeline.
 */
// TODO: 1. wait for node manager 2. add node stat
@Component
@Scope("prototype")
class HandshakeHandler @Autowired constructor(
    val cfg: AppConfig
): ByteToMessageDecoder(){
    companion object {
        val loggerWire: Logger = LoggerFactory.getLogger("wire")
        val loggerNet: Logger = LoggerFactory.getLogger("net")
    }

    private var frameCodec: FrameCodec? = null
    private var channel: Channel? = null
    var remoteId: ByteArray? = null
    private val myKey = cfg.myKey
    private var nodeId: ByteArray? = null
    private var handshake: EncryptionHandshake? = null
    private var initiatePacket: ByteArray? = null
    private var isHandshakeDone = false

    fun setRemote(remoteId: String, channel: ChannelImpl) {
        this.remoteId = HexBytes.decode(remoteId)
        this.channel = channel
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        channel!!.inetSocketAddress = ctx.channel().remoteAddress() as InetSocketAddress
        if (remoteId!!.size == 64) {
            channel!!.initWithNode(remoteId!!)
            initiate(ctx)
        } else {
            handshake = EncryptionHandshake()
            nodeId = myKey.nodeId
        }
    }

    // decode into objects
    override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: List<Any>) {
        loggerWire.debug("Decoding handshake... (" + input.readableBytes() + " bytes available)")
        decodeHandshake(ctx, input)
        if (isHandshakeDone) {
            loggerWire.debug("Handshake done, removing HandshakeHandler from pipeline.")
            ctx.pipeline().remove(this)
        }
    }

    fun initiate(ctx: ChannelHandlerContext) {
        loggerNet.debug("RLPX protocol activated")
        nodeId = myKey.nodeId
        handshake = EncryptionHandshake(ECKey.fromNodeId(this.remoteId).pubKeyPoint)
        val msg: Any

        if (cfg.eip8) {
            val initiateMessage: AuthInitiateMessageV4 = handshake!!.createAuthInitiateV4(myKey)
            initiatePacket = handshake!!.encryptAuthInitiateV4(initiateMessage)
            msg = initiateMessage
        } else {
            val initiateMessage: AuthInitiateMessage = handshake!!.createAuthInitiate(null, myKey)
            initiatePacket = handshake!!.encryptAuthMessage(initiateMessage)
            msg = initiateMessage
        }
        val byteBufMsg = ctx.alloc().buffer(initiatePacket!!.size)
        byteBufMsg.writeBytes(initiatePacket)
        ctx.writeAndFlush(byteBufMsg).sync()

//        channel.getNodeStatistics().rlpxAuthMessagesSent.add()

        if (loggerNet.isDebugEnabled()) loggerNet.debug(
            "To:   {}    Send:  {}",
            ctx.channel().remoteAddress(),
            msg
        )
    }

    // consume handshake, producing no resulting message to upper layers
    private fun decodeHandshake(ctx: ChannelHandlerContext, buffer: ByteBuf) {
        val handshake = this.handshake!!

        if (handshake.isInitiator) {
            if (this.frameCodec == null) {
                var responsePacket: ByteArray? = ByteArray(AuthResponseMessage.getLength() + ECIESCoder.getOverhead())
                if (!buffer.isReadable(responsePacket!!.size)) return
                buffer.readBytes(responsePacket)
                try {

                    // trying to decode as pre-EIP-8
                    val response: AuthResponseMessage =
                        handshake.handleAuthResponse(myKey, initiatePacket, responsePacket)
                    loggerNet.debug("From: {}    Recv:  {}", ctx.channel().remoteAddress(), response)
                } catch (t: Throwable) {

                    // it must be format defined by EIP-8 then
                    responsePacket = readEIP8Packet(buffer, responsePacket)
                    if (responsePacket.isEmpty()) return
                    val response: AuthResponseMessageV4 =
                        handshake.handleAuthResponseV4(myKey, initiatePacket, responsePacket)
                    loggerNet.debug("From: {}    Recv:  {}", ctx.channel().remoteAddress(), response)
                }
                val secrets: EncryptionHandshake.Secrets = handshake.secrets
                this.frameCodec = FrameCodec(secrets)
                loggerNet.debug("auth exchange done")
//                channel.sendHelloMessage(ctx, frameCodec, Hex.toHexString(nodeId))
            } else {
                val frameCodec = this.frameCodec!!
                loggerWire.info("MessageCodec: Buffer bytes: " + buffer.readableBytes())
                val frames: List<Frame> = frameCodec.readFrames(buffer)
                if ( frames.isEmpty()) return
                val frame: Frame = frames[0]
                val payload = ByteStreams.toByteArray(frame.stream)
                if (frame.type == P2pMessageCodes.HELLO.cmd) {
                    val helloMessage = Rlp.decode(payload, HelloMessage::class.java)
                    if (loggerNet.isDebugEnabled) loggerNet.debug(
                        "From: {}    Recv:  {}",
                        ctx.channel().remoteAddress(),
                        helloMessage
                    )
                    isHandshakeDone = true
//                    this.channel.publicRLPxHandshakeFinished(ctx, frameCodec, helloMessage)
                } else {
//                    val message = DisconnectMessage(payload)
//                    if (loggerNet.isDebugEnabled()) loggerNet.debug(
//                        "From: {}    Recv:  {}",
//                        channel,
//                        message
//                    )
//                    channel.getNodeStatistics().nodeDisconnectedRemote(message.getReason())
                }
            }
        } else {
            loggerWire.debug("Not initiator.")
            if (frameCodec == null) {
                loggerWire.debug("FrameCodec == null")
                var authInitPacket: ByteArray? = ByteArray(AuthInitiateMessage.getLength() + ECIESCoder.getOverhead())
                if (!buffer.isReadable(authInitPacket!!.size)) return
                buffer.readBytes(authInitPacket)
                this.handshake = EncryptionHandshake()
                var responsePacket: ByteArray
                try {

                    // trying to decode as pre-EIP-8
                    val initiateMessage: AuthInitiateMessage = handshake.decryptAuthInitiate(authInitPacket, myKey)
                    loggerNet.debug(
                        "From: {}    Recv:  {}",
                        ctx.channel().remoteAddress(),
                        initiateMessage
                    )
                    val response: AuthResponseMessage = handshake.makeAuthInitiate(initiateMessage, myKey)
                    loggerNet.debug("To:   {}    Send:  {}", ctx.channel().remoteAddress(), response)
                    responsePacket = handshake.encryptAuthResponse(response)
                } catch (t: Throwable) {

                    // it must be format defined by EIP-8 then
                    try {
                        authInitPacket = readEIP8Packet(buffer, authInitPacket)
                        if (authInitPacket.isEmpty()) return
                        val initiateMessage: AuthInitiateMessageV4 =
                            handshake.decryptAuthInitiateV4(authInitPacket, myKey)
                        loggerNet.debug(
                            "From: {}    Recv:  {}",
                            ctx.channel().remoteAddress(),
                            initiateMessage
                        )
                        val response: AuthResponseMessageV4 = handshake.makeAuthInitiateV4(initiateMessage, myKey)
                        loggerNet.debug(
                            "To:   {}    Send:  {}",
                            ctx.channel().remoteAddress(),
                            response
                        )
                        responsePacket = handshake.encryptAuthResponseV4(response)
                    } catch (ce: InvalidCipherTextException) {
                        loggerNet.warn(
                            "Can't decrypt AuthInitiateMessage from " + ctx.channel().remoteAddress() +
                                    ". Most likely the remote peer used wrong public key (NodeID) to encrypt message."
                        )
                        return
                    }
                }
                handshake.agreeSecret(authInitPacket, responsePacket)
                val secrets: EncryptionHandshake.Secrets = handshake.secrets
                this.frameCodec = FrameCodec(secrets)
                val remotePubKey: ECPoint = handshake.remotePublicKey
                val compressed = remotePubKey.encoded
                this.remoteId = ByteArray(compressed.size - 1)
                System.arraycopy(compressed, 1, this.remoteId!!, 0, this.remoteId!!.size)
                val byteBufMsg = ctx.alloc().buffer(responsePacket.size)
                byteBufMsg.writeBytes(responsePacket)
                ctx.writeAndFlush(byteBufMsg).sync()
            } else {
//                val frameCodec = this.frameCodec!!
//                val frames: List<Frame> = frameCodec.readFrames(buffer)
//                if (frames.isEmpty()) return
//                val frame: Frame = frames[0]
//                val message: Message = P2pMessageFactory().create(
//                    frame.getType() as Byte,
//                    ByteStreams.toByteArray(frame.getStream())
//                )
//                loggerNet.debug("From: {}    Recv:  {}", ctx.channel().remoteAddress(), message)
//                if (frame.getType() === P2pMessageCodes.DISCONNECT.asByte()) {
//                    loggerNet.debug("Active remote peer disconnected right after handshake.")
//                    return
//                }
//                if (frame.getType() !== P2pMessageCodes.HELLO.asByte()) {
//                    throw RuntimeException("The message type is not HELLO or DISCONNECT: $message")
//                }
//                val inboundHelloMessage: HelloMessage = message as HelloMessage
//
//                // now we know both remote nodeId and port
//                // let's set node, that will cause registering node in NodeManager
//                channel.initWithNode(remoteId, inboundHelloMessage.getListenPort())
//
//                // Secret authentication finish here
//                channel.sendHelloMessage(ctx, frameCodec, Hex.toHexString(nodeId))
//                isHandshakeDone = true
//                this.channel.publicRLPxHandshakeFinished(ctx, frameCodec, inboundHelloMessage)
//                channel.getNodeStatistics().rlpxInHello.add()
            }
        }
    }

    private fun readEIP8Packet(buffer: ByteBuf, plainPacket: ByteArray): ByteArray {
        val size: Int = BigEndian.decodeInt16(plainPacket, 0).toUShort().toInt()
        if (size < plainPacket.size)
            throw IllegalArgumentException("AuthResponse packet size is too low")
        val bytesLeft = size - plainPacket.size + 2
        val restBytes = ByteArray(bytesLeft)
        if (!buffer.isReadable(restBytes.size)) return ByteUtil.EMPTY_BYTE_ARRAY
        buffer.readBytes(restBytes)
        val fullResponse = ByteArray(size + 2)
        System.arraycopy(plainPacket, 0, fullResponse, 0, plainPacket.size)
        System.arraycopy(restBytes, 0, fullResponse, plainPacket.size, restBytes.size)
        return fullResponse
    }


    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
//        if (channel.discoveryMode) {
//            loggerNet.trace("Handshake failed: $cause")
//        } else {
//            if (cause is IOException || cause is ReadTimeoutException) {
//                loggerNet.debug("Handshake failed: " + ctx.channel().remoteAddress() + ": " + cause)
//            } else {
//                loggerNet.warn("Handshake failed: ", cause)
//            }
//        }
//        ctx.close()
    }
}