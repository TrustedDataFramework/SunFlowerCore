package org.tdf.sunflower.p2pv2.eth

import java.util.*


enum class EthVersion(val code: Byte) {
    V62(62.toByte()), V63(63.toByte());

    fun isCompatible(version: EthVersion): Boolean {
        return if (version.code >= code) {
            this.code >= code
        } else {
            this.code < code
        }
    }

    companion object {
        val LOWER = V62.code
        val UPPER = V63.code
        fun fromCode(code: Int): EthVersion? {
            for (v in values()) {
                if (v.code.toInt() == code) {
                    return v
                }
            }
            return null
        }

        fun isSupported(code: Byte): Boolean {
            return code >= LOWER && code <= UPPER
        }

        fun supported(): List<EthVersion> {
            val supported: MutableList<EthVersion> = ArrayList()
            for (v in values()) {
                if (isSupported(v.code)) {
                    supported.add(v)
                }
            }
            return supported
        }
    }
}