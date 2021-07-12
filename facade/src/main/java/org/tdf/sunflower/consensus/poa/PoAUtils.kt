package org.tdf.sunflower.consensus.poa

import org.tdf.common.crypto.ECKey
import org.tdf.common.types.Constants.WORD_SIZE
import org.tdf.common.util.*
import org.tdf.sunflower.types.Header
import org.tdf.sunflower.types.HeaderImpl.Companion.emptyNonce

object PoAUtils {
    fun getRawHash(header: Header): ByteArray {
        return header.impl.copy(extraData = HexBytes.empty(), mixHash = ByteUtil.ZEROS_32, nonce = emptyNonce).rlp().sha3()
    }

    fun sign(key: ECKey, header: Header): Header {
        val hash = getRawHash(header)
        val sig = key.sign(hash)
        val l = java.lang.Byte.toUnsignedLong(sig.v)
        return header.impl.copy(extraData = sig.r.bytes(WORD_SIZE).hex(), mixHash = sig.s.bytes(WORD_SIZE).hex(), nonce = l.bytes8().hex())
    }
}