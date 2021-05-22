package org.tdf.sunflower.p2pv2.swarm.bzz

import org.tdf.sunflower.p2pv2.MessageCode

enum class BzzMessageCodes(override val code: Int) : MessageCode {
    /**
     * Handshake BZZ message
     */
    STATUS(0x00),

    /**
     * Request to store a [org.ethereum.net.swarm.Chunk]
     */
    STORE_REQUEST(0x01),

    /**
     * Used for several purposes
     * - the main is to ask for a [org.ethereum.net.swarm.Chunk] with the specified hash
     * - ask to send back {#PEERS} message with the known nodes nearest to the specified hash
     * - initial request after handshake with zero hash. On this request the nearest known
     * neighbours are sent back with the {#PEERS} message.
     */
    RETRIEVE_REQUEST(0x02),

    /**
     * The message is the immediate response on the {#RETRIEVE_REQUEST} with the nearest known nodes
     * of the requested hash.
     */
    PEERS(0x03);

    companion object {
        private val intToTypeMap = arrayOf(STATUS, STORE_REQUEST, RETRIEVE_REQUEST, PEERS)

        fun fromInt(i: Int): BzzMessageCodes {
            return intToTypeMap[i]
        }

        fun inRange(code: Int): Boolean {
            return code >= STATUS.code && code <= PEERS.code
        }
    }

}
