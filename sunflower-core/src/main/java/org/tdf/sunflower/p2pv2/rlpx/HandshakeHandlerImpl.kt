package org.tdf.sunflower.p2pv2.rlpx

import com.google.common.io.ByteStreams
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
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
import org.tdf.sunflower.p2pv2.Loggers
import org.tdf.sunflower.p2pv2.P2pMessageCodes
import org.tdf.sunflower.p2pv2.message.Message
import org.tdf.sunflower.p2pv2.p2p.DisconnectMessage
import org.tdf.sunflower.p2pv2.p2p.HelloMessage
import org.tdf.sunflower.p2pv2.rlpx.discover.NodeManager
import org.tdf.sunflower.p2pv2.server.Channel
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
class HandshakeHandlerImpl @Autowired constructor(
    private val cfg: AppConfig,
    private val nodeManager: NodeManager
): HandshakeHandler(), Loggers{

    private var frameCodec: FrameCodec? = null

    private var _channel: Channel? = null
    val channel: Channel
        get() = _channel!!

    private var _remoteId: ByteArray? = null
    override val remoteId: ByteArray
        get() = _remoteId!!


    private val myKey = cfg.myKey

    private var _nodeId: ByteArray? = null
    private val nodeId: ByteArray
        get() = _nodeId!!

    private var _handshake: EncryptionHandshake? = null
    private val handshake: EncryptionHandshake
        get() = _handshake!!



    private var _initiatePacket: ByteArray? = null
    private val initiatePacket: ByteArray
        get() = _initiatePacket!!

    private var isHandshakeDone = false

    override fun setRemote(remoteId: String, channel: Channel) {
        this._remoteId = HexBytes.decode(remoteId)
        this._channel = channel
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        channel.inetSocketAddress = ctx.channel().remoteAddress() as InetSocketAddress
        if (remoteId.size == 64) {
            channel.initWithNode(remoteId)
            initiate(ctx)
        } else {
            _handshake = EncryptionHandshake()
            _nodeId = myKey.nodeId
        }
    }

    // decode into objects
    override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: List<Any>) {
        wire.debug("Decoding handshake... (" + input.readableBytes() + " bytes available)")
        decodeHandshake(ctx, input)
        if (isHandshakeDone) {
            wire.debug("Handshake done, removing HandshakeHandler from pipeline.")
            ctx.pipeline().remove(this)
        }
    }

    override fun initiate(ctx: ChannelHandlerContext) {
        net.debug("RLPX protocol activated")
        _nodeId = myKey.nodeId

        _handshake = EncryptionHandshake(ECKey.fromNodeId(this.remoteId).pubKeyPoint)
        val msg: Any

        if (cfg.eip8) {
            val initiateMessage: AuthInitiateMessageV4 = handshake.createAuthInitiateV4(myKey)
            _initiatePacket = handshake.encryptAuthInitiateV4(initiateMessage)
            msg = initiateMessage
        } else {
            val initiateMessage: AuthInitiateMessage = handshake.createAuthInitiate(null, myKey)
            _initiatePacket = handshake.encryptAuthMessage(initiateMessage)
            msg = initiateMessage
        }
        val byteBufMsg = ctx.alloc().buffer(initiatePacket.size)
        byteBufMsg.writeBytes(initiatePacket)
        ctx.writeAndFlush(byteBufMsg).sync()

        channel.nodeStatistics.rlpxAuthMessagesSent.add()

        if (net.isDebugEnabled) net.debug(
            "To:   {}    Send:  {}",
            ctx.channel().remoteAddress(),
            msg
        )
    }

    // consume handshake, producing no resulting message to upper layers
    private fun decodeHandshake(ctx: ChannelHandlerContext, buffer: ByteBuf) {
        if (handshake.isInitiator) {
            if (this.frameCodec == null) {
                dev.info("frameCodec is null, try to assign value to frameCodec")
                var responsePacket = ByteArray(AuthResponseMessage.getLength() + ECIESCoder.getOverhead())
                dev.info("try to read response from buffer size = ${responsePacket.size}")
                if (!buffer.isReadable(responsePacket.size)) {
                    dev.error("response packet has not fully read")
                    return
                }
                buffer.readBytes(responsePacket)
                try {
                    // trying to decode as pre-EIP-8
                    val response = handshake.handleAuthResponse(myKey, initiatePacket, responsePacket)
                    dev.info("read response from handshake success")
                    net.debug("From: {}    Recv:  {}", ctx.channel().remoteAddress(), response)
                } catch (t: Throwable) {
                    // it must be format defined by EIP-8 then
                    responsePacket = readEIP8Packet(buffer, responsePacket)
                    if (responsePacket.isEmpty()) return
                    val response = handshake.handleAuthResponseV4(myKey, initiatePacket, responsePacket)
                    net.debug("From: {}    Recv:  {}", ctx.channel().remoteAddress(), response)
                }
                dev.info("try to create frameCodec")
                val secrets = handshake.secrets
                this.frameCodec = FrameCodec(secrets)
                dev.info("auth exchange done")
                net.debug("auth exchange done")
                dev.info("send hello message to remote peer")
                channel.sendHelloMessage(ctx, frameCodec!!, HexBytes.encode(nodeId))
            } else {
                val frameCodec = this.frameCodec!!
                wire.info("MessageCodec: Buffer bytes: " + buffer.readableBytes())
                val frames: List<Frame> = frameCodec.readFrames(buffer)
                dev.info("receive frames from remote ")
                if (frames.isEmpty()) {
                    dev.info("received frames is empty")
                }
                val frame: Frame = frames[0]
                val payload = ByteStreams.toByteArray(frame.stream)
                if (frame.type == P2pMessageCodes.HELLO.code) {
                    val helloMessage = Rlp.decode(payload, HelloMessage::class.java)
                    dev.info("received hello message $helloMessage")

                    if (net.isDebugEnabled) net.debug(
                        "From: {}    Recv:  {}",
                        ctx.channel().remoteAddress(),
                        helloMessage
                    )
                    dev.info("finish handshake")
                    isHandshakeDone = true
                    this.channel.finishHandshake(ctx, frameCodec, helloMessage)
                } else {
                    val message = Rlp.decode(payload, DisconnectMessage::class.java)
                    dev.info("received disconnect message from remote reason = ${message.reason}")
                    if (net.isDebugEnabled) net.debug(
                        "From: {}    Recv:  {}",
                        channel,
                        message
                    )
                    channel.nodeStatistics.nodeDisconnectedRemote(message.reason)
                }
            }
        } else {
            wire.debug("Not initiator.")
            dev.info("Not initiator.")
            if (frameCodec == null) {
                wire.debug("FrameCodec == null")
                dev.info("FrameCodec == null ")
                var authInitPacket: ByteArray? = ByteArray(AuthInitiateMessage.getLength() + ECIESCoder.getOverhead())
                if (!buffer.isReadable(authInitPacket!!.size)) return
                buffer.readBytes(authInitPacket)
                _handshake = EncryptionHandshake()
                var responsePacket: ByteArray
                try {

                    // trying to decode as pre-EIP-8
                    val initiateMessage: AuthInitiateMessage = handshake.decryptAuthInitiate(authInitPacket, myKey)
                    net.debug(
                        "From: {}    Recv:  {}",
                        ctx.channel().remoteAddress(),
                        initiateMessage
                    )
                    val response: AuthResponseMessage = handshake.makeAuthInitiate(initiateMessage, myKey)
                    net.debug("To:   {}    Send:  {}", ctx.channel().remoteAddress(), response)
                    responsePacket = handshake.encryptAuthResponse(response)
                } catch (t: Throwable) {

                    // it must be format defined by EIP-8 then
                    try {
                        authInitPacket = readEIP8Packet(buffer, authInitPacket)
                        if (authInitPacket.isEmpty()) return
                        val initiateMessage: AuthInitiateMessageV4 =
                            handshake.decryptAuthInitiateV4(authInitPacket, myKey)
                        net.debug(
                            "From: {}    Recv:  {}",
                            ctx.channel().remoteAddress(),
                            initiateMessage
                        )
                        val response: AuthResponseMessageV4 = handshake.makeAuthInitiateV4(initiateMessage, myKey)
                        net.debug(
                            "To:   {}    Send:  {}",
                            ctx.channel().remoteAddress(),
                            response
                        )
                        responsePacket = handshake.encryptAuthResponseV4(response)
                    } catch (ce: InvalidCipherTextException) {
                        net.warn(
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
                this._remoteId = ByteArray(compressed.size - 1)
                System.arraycopy(compressed, 1, this.remoteId, 0, this.remoteId.size)
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
//                net.debug("From: {}    Recv:  {}", ctx.channel().remoteAddress(), message)
//                if (frame.type == P2pMessageCodes.DISCONNECT.code) {
//                    net.debug("Active remote peer disconnected right after handshake.")
//                    return
//                }
//                if (frame.type != P2pMessageCodes.HELLO.code) {
//                    throw RuntimeException("The message type is not HELLO or DISCONNECT: $message")
//                }
//                val inboundHelloMessage: HelloMessage = message as HelloMessage
//
//                // now we know both remote nodeId and port
//                // let's set node, that will cause registering node in NodeManager
//                channel.initWithNode(remoteId, inboundHelloMessage.listenPort)
//
//                // Secret authentication finish here
//                channel.sendHelloMessage(ctx, frameCodec, HexBytes.encode(nodeId))
//                isHandshakeDone = true
//                this.channel.finishHandshake(ctx, frameCodec, inboundHelloMessage)
//                channel.nodeStatistics.rlpxInHello.add()
            }
        }
    }

    private fun readEIP8Packet(buffer: ByteBuf, plainPacket: ByteArray): ByteArray {
        val size: Int = BigEndian.decodeInt16(plainPacket, 0).toUShort().toInt()
        if (size < plainPacket.size)
            throw IllegalArgumentException("AuthResponse packet size is too low")
        val bytesLeft = size - plainPacket.size + 2
        val restBytes = ByteArray(bytesLeft)
        if (!buffer.isReadable(restBytes.size))
            return ByteUtil.EMPTY_BYTE_ARRAY
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