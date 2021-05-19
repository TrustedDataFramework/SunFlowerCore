package org.tdf.sunflower.p2pv2.rlpx

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.ByteBufOutputStream
import org.spongycastle.crypto.BlockCipher
import org.spongycastle.crypto.StreamCipher
import org.spongycastle.crypto.digests.KeccakDigest
import org.spongycastle.crypto.engines.AESEngine
import org.spongycastle.crypto.modes.SICBlockCipher
import org.spongycastle.crypto.params.KeyParameter
import org.spongycastle.crypto.params.ParametersWithIV
import org.tdf.common.util.RLPUtil
import org.tdf.rlpstream.Rlp
import org.tdf.rlpstream.RlpStream
import org.tdf.rlpstream.StreamId
import java.io.*
import java.util.*
import kotlin.experimental.xor

class FrameCodec(secrets: EncryptionHandshake.Secrets) {
    private val enc: StreamCipher
    private val dec: StreamCipher
    private val egressMac: KeccakDigest = secrets.egressMac
    private val ingressMac: KeccakDigest = secrets.ingressMac
    private val mac: ByteArray = secrets.mac
    private var isHeadRead = false
    private var totalBodySize = 0
    private var contextId = -1
    private var totalFrameSize = -1
    private var protocol = 0
    private fun makeMacCipher(): AESEngine {
        // Stateless AES encryption
        val macc = AESEngine()
        macc.init(true, KeyParameter(mac))
        return macc
    }

    fun writeFrame(frame: Frame, buf: ByteBuf?) {
        writeFrame(frame, ByteBufOutputStream(buf))
    }

    fun writeFrame(frame: Frame, out: OutputStream) {
        val headBuffer = ByteArray(32)
        val ptype = Rlp.encodeInt(frame.type) // FIXME encodeLong
        val totalSize = frame.size + ptype.size
        headBuffer[0] = (totalSize shr 16).toByte()
        headBuffer[1] = (totalSize shr 8).toByte()
        headBuffer[2] = totalSize.toByte()
        val headerDataElems: MutableList<ByteArray> = ArrayList()
        headerDataElems.add(Rlp.encodeInt(0))
        if (frame.contextId >= 0) headerDataElems.add(Rlp.encodeInt(frame.contextId))
        if (frame.totalFrameSize >= 0) headerDataElems.add(Rlp.encodeInt(frame.totalFrameSize))
        val headerData = Rlp.encodeElements(headerDataElems)
        System.arraycopy(headerData, 0, headBuffer, 3, headerData.size)
        enc.processBytes(headBuffer, 0, 16, headBuffer, 0)

        // Header MAC
        updateMac(egressMac, headBuffer, 0, headBuffer, 16, true)
        val buff = ByteArray(256)
        out.write(headBuffer)
        enc.processBytes(ptype, 0, ptype.size, buff, 0)
        out.write(buff, 0, ptype.size)
        egressMac.update(buff, 0, ptype.size)
        while (true) {
            val n = frame.stream.read(buff)
            if (n <= 0) break
            enc.processBytes(buff, 0, n, buff, 0)
            egressMac.update(buff, 0, n)
            out.write(buff, 0, n)
        }
        val padding = 16 - totalSize % 16
        val pad = ByteArray(16)
        if (padding < 16) {
            enc.processBytes(pad, 0, padding, buff, 0)
            egressMac.update(buff, 0, padding)
            out.write(buff, 0, padding)
        }

        // Frame MAC
        val macBuffer = ByteArray(egressMac.digestSize)
        doSum(egressMac, macBuffer) // fmacseed
        updateMac(egressMac, macBuffer, 0, macBuffer, 0, true)
        out.write(macBuffer, 0, 16)
    }

    fun readFrames(buf: ByteBuf?): List<Frame> {
        ByteBufInputStream(buf).use { bufInputStream -> return readFrames(bufInputStream) }
    }

    fun readFrames(inp: DataInput): List<Frame> {
        if (!isHeadRead) {
            val headBuffer = ByteArray(32)
            try {
                inp.readFully(headBuffer)
            } catch (e: EOFException) {
                return emptyList()
            }

            // Header MAC
            updateMac(ingressMac, headBuffer, 0, headBuffer, 16, false)
            dec.processBytes(headBuffer, 0, 16, headBuffer, 0)
            totalBodySize = headBuffer[0].toUByte().toInt()
            totalBodySize = (totalBodySize shl 8) + (headBuffer[1].toUByte().toInt())
            totalBodySize = (totalBodySize shl 8) + (headBuffer[2].toUByte().toInt())
            val rlpList = RLPUtil.decodePartial(headBuffer, 3)
            protocol = Rlp.decodeInt(rlpList.rawAt(0))
            contextId = -1
            totalFrameSize = -1
            if (rlpList.size() > 1) {
                contextId = Rlp.decodeInt(rlpList.rawAt(1))
                if (rlpList.size() > 2) {
                    totalFrameSize = Rlp.decodeInt(rlpList.rawAt(2))
                }
            }
            isHeadRead = true
        }
        var padding = 16 - totalBodySize % 16
        if (padding == 16) padding = 0
        val macSize = 16
        val buffer = ByteArray(totalBodySize + padding + macSize)
        try {
            inp.readFully(buffer)
        } catch (e: EOFException) {
            return emptyList()
        }
        val frameSize = buffer.size - macSize
        ingressMac.update(buffer, 0, frameSize)
        dec.processBytes(buffer, 0, frameSize, buffer, 0)
        var pos = 0
        val typeStreamId = RlpStream.decodeElement(buffer, pos, buffer.size, false)
        val type = RlpStream.asLong(buffer, typeStreamId)
        pos = StreamId.offsetOf(typeStreamId) + StreamId.sizeOf(typeStreamId)
        val payload: InputStream = ByteArrayInputStream(buffer, pos, totalBodySize - pos)
        val size = totalBodySize - pos
        val macBuffer = ByteArray(ingressMac.digestSize)

        // Frame MAC
        doSum(ingressMac, macBuffer) // fmacseed
        updateMac(ingressMac, macBuffer, 0, buffer, frameSize, false)
        isHeadRead = false
        val frame = Frame(type.toInt(), size, payload)
        frame.contextId = contextId
        frame.totalFrameSize = totalFrameSize
        return listOf(frame)
    }

    private fun updateMac(
        mac: KeccakDigest,
        seed: ByteArray,
        offset: Int,
        out: ByteArray,
        outOffset: Int,
        egress: Boolean
    ): ByteArray {
        val aesBlock = ByteArray(mac.digestSize)
        doSum(mac, aesBlock)
        makeMacCipher().processBlock(aesBlock, 0, aesBlock, 0)
        // Note that although the mac digest size is 32 bytes, we only use 16 bytes in the computation
        val length = 16
        for (i in 0 until length) {
            aesBlock[i] = aesBlock[i] xor (seed[i + offset])
        }
        mac.update(aesBlock, 0, length)
        val result = ByteArray(mac.digestSize)
        doSum(mac, result)
        if (egress) {
            System.arraycopy(result, 0, out, outOffset, length)
        } else {
            for (i in 0 until length) {
                if (out[i + outOffset] != result[i]) {
                    throw IOException("MAC mismatch")
                }
            }
        }
        return result
    }

    private fun doSum(mac: KeccakDigest, out: ByteArray) {
        // doFinal without resetting the MAC by using clone of digest state
        KeccakDigest(mac).doFinal(out, 0)
    }

    init {
        var cipher: BlockCipher = AESEngine()
        enc = SICBlockCipher(cipher)
        enc.init(true, ParametersWithIV(KeyParameter(secrets.aes), ByteArray(cipher.blockSize)))
        cipher = AESEngine()
        dec = SICBlockCipher(cipher)
        dec.init(false, ParametersWithIV(KeyParameter(secrets.aes), ByteArray(cipher.blockSize)))
    }
}