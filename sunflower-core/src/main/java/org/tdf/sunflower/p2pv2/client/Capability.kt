package org.tdf.sunflower.p2pv2.client

import org.tdf.rlpstream.RlpCreator
import org.tdf.rlpstream.RlpProps

@RlpProps("name", "version")
data class Capability @RlpCreator constructor(val name: String, val version: Byte): Comparable<Capability>{
    companion object {
        const val P2P = "p2p"
        const val ETH = "eth"
        const val SHH = "shh"
        const val BZZ = "bzz"
    }

    override fun compareTo(other: Capability): Int {
        val cmp = name.compareTo(other.name)
        return if (cmp != 0) {
            cmp
        } else {
            version.compareTo(other.version)
        }
    }
}