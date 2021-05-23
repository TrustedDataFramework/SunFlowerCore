package org.tdf.sunflower.p2pv2.client

import com.github.salpadding.rlpstream.RlpCreator
import com.github.salpadding.rlpstream.RlpProps

@RlpProps("name", "version")
data class Capability @RlpCreator constructor(val name: String, val version: Int) : Comparable<Capability> {
    companion object {
        const val P2P = "p2p"
        const val ETH = "eth"
        const val SHH = "shh"
        const val BZZ = "bzz"
    }

    val isEth: Boolean
        get() = name == ETH

    override fun compareTo(other: Capability): Int {
        val cmp = name.compareTo(other.name)
        return if (cmp != 0) {
            cmp
        } else {
            version.compareTo(other.version)
        }
    }
}