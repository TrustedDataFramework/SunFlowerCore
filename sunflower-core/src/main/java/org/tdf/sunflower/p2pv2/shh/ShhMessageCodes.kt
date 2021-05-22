package org.tdf.sunflower.p2pv2.shh

import org.tdf.sunflower.p2pv2.MessageCode

/**
 * A list of commands for the Whisper network protocol.
 * <br></br>
 * The codes for these commands are the first byte in every packet.
 *
 * @see [
 * https://github.com/ethereum/wiki/wiki/Wire-Protocol](https://github.com/ethereum/wiki/wiki/Wire-Protocol)
 */
enum class ShhMessageCodes(override val code: Int) : MessageCode {
    /* Whisper Protocol */ /**
     * [+0x00]
     */
    STATUS(0x00),

    /**
     * [+0x01]
     */
    MESSAGE(0x01),

    /**
     * [+0x02]
     */
    FILTER(0x02);

    companion object {
        private val intToTypeMap = arrayOf(STATUS, MESSAGE, FILTER)

        fun fromInt(i: Int): ShhMessageCodes {
            return intToTypeMap[i]
        }

        fun inRange(code: Int): Boolean {
            return code >= STATUS.code && code <= FILTER.code
        }
    }
}