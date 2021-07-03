package org.tdf.common.serialize

import com.github.salpadding.rlpstream.Rlp
import org.tdf.common.serialize.Codec.Companion.newInstance
import org.tdf.common.util.HexBytes
import java.nio.charset.StandardCharsets
import java.util.function.Function

object Codecs {
    /**
     * Converter from string to byte array and vice versa
     */
    @JvmField
    val STRING = newInstance(
        { it.toByteArray(StandardCharsets.UTF_8) }
    ) { String(it, StandardCharsets.UTF_8) }

    var IDENTITY: Codec<*> = newInstance(Function.identity(), Function.identity())

    @JvmField
    var HEX = newInstance({ it.bytes }) { HexBytes.fromBytes(it) }

    @JvmStatic
    fun <K> newRLPCodec(clazz: Class<K>): Codec<K> {
        return newInstance({ Rlp.encode(it) }) { Rlp.decode(it, clazz) }
    }
}