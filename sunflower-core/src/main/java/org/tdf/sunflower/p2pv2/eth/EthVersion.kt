package org.tdf.sunflower.p2pv2.eth

import java.util.*


enum class EthVersion(val code: Int) {
    V62(62), V63(63);

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

        @JvmStatic
        fun fromCode(code: Int): EthVersion {
            for (v in values()) {
                if (v.code == code) {
                    return v
                }
            }
            throw RuntimeException("eth version $code not found")
        }

        @JvmStatic
        fun isSupported(code: Int): Boolean {
            return code >= LOWER && code <= UPPER
        }

        @JvmStatic
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