package org.tdf.sunflower.p2pv2.shh

import java.util.*

/**
 * A list of commands for the Whisper network protocol.
 * <br></br>
 * The codes for these commands are the first byte in every packet.
 *
 * @see [
 * https://github.com/ethereum/wiki/wiki/Wire-Protocol](https://github.com/ethereum/wiki/wiki/Wire-Protocol)
 */
enum class ShhMessageCodes(private val cmd: Int) {
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
        private val intToTypeMap: Array<ShhMessageCodes?> = arrayOfNulls(3)

        fun fromByte(i: Int): ShhMessageCodes {
            return intToTypeMap[i]!!
        }

        fun inRange(code: Int): Boolean {
            return code >= STATUS.cmd && code <= FILTER.cmd
        }

        init {
            for (type in values()) {
                intToTypeMap[type.cmd] = type
            }
        }
    }

}