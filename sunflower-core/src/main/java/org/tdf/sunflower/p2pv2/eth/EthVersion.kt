package org.tdf.sunflower.p2pv2.eth

import org.tdf.sunflower.p2pv2.eth.message.EthMessageCodes


enum class EthVersion(val code: Int, val maxMsgCode: Int) {
    V62(62, 0x07), V63(63, 0x10), V64(64, 0x10), V65(65, 0x0a);

    fun isCompatible(code: Int): Boolean {
        return EthMessageCodes.fromInt(code).isCompatible(this)
    }

    companion object {
        val LOWER = V62.code
        val UPPER = V65.code

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