package org.tdf.common.serialize

import java.util.function.Function

/**
 * encoder and decoder from a type to another type
 *
 * @param <K> type
</K> */
interface Codec<K> {
    val encoder: Function<in K, ByteArray>
    val decoder: Function<ByteArray, out K>

    companion object {
        @JvmStatic
        fun identity(): Codec<ByteArray> {
            return Codecs.IDENTITY as Codec<ByteArray>
        }

        fun <K> newInstance(
            encoder: Function<in K, ByteArray>,
            decoder: Function<ByteArray, out K>
        ): Codec<K> {
            return object : Codec<K> {
                override val encoder: Function<in K, ByteArray>
                    get() = encoder
                override val decoder: Function<ByteArray, out K>
                    get() = decoder
            }
        }
    }
}