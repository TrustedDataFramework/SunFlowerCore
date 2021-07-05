package org.tdf.common.serialize

import com.github.salpadding.rlpstream.Rlp
import org.tdf.common.serialize.Codec.Companion.create
import org.tdf.common.util.HexBytes
import java.nio.charset.StandardCharsets
import java.util.function.Function

object Codecs {
    /**
     * Converter from string to byte array and vice versa
     */
    @JvmField
    val string = create(
        { it.toByteArray(StandardCharsets.UTF_8) }
    ) { String(it, StandardCharsets.UTF_8) }

    val identity: Codec<*> = create(Function.identity(), Function.identity())

    val hex = create({ it.bytes }) { HexBytes.fromBytes(it) }

    @JvmStatic
    fun <K> rlp(clazz: Class<K>): Codec<K> {
        return create({ Rlp.encode(it) }) { Rlp.decode(it, clazz) }
    }
}