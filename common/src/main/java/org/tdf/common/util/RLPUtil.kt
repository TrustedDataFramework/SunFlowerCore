package org.tdf.common.util

import com.github.salpadding.rlpstream.Rlp
import com.github.salpadding.rlpstream.RlpList
import com.github.salpadding.rlpstream.StreamId

object RLPUtil {
    @JvmStatic
    fun <T> decode(data: HexBytes, clazz: Class<T>): T {
        return Rlp.decode(data.bytes, clazz)
    }

    @JvmStatic
    fun encode(o: Any?): HexBytes {
        return HexBytes.fromBytes(Rlp.encode(o))
    }

    @JvmStatic
    fun decodePartial(bin: ByteArray, offset: Int): RlpList {
        val streamId = StreamId.decodeElement(bin, offset, bin.size, false)
        return StreamId.asList(bin, streamId)
    }
}