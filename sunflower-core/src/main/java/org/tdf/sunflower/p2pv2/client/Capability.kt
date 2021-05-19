package org.tdf.sunflower.p2pv2.client

import org.tdf.rlpstream.RlpCreator
import org.tdf.rlpstream.RlpProps

@RlpProps("name", "version")
data class Capability @RlpCreator constructor(val name: String, val version: Byte) {
    companion object {
        const val P2P = "p2p"
        const val ETH = "eth"
        const val SHH = "shh"
        const val BZZ = "bzz"
    }
}