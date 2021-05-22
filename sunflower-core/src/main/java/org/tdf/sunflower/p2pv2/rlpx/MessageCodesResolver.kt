package org.tdf.sunflower.p2pv2.rlpx

import org.tdf.sunflower.p2pv2.P2pMessageCodes
import org.tdf.sunflower.p2pv2.client.Capability
import org.tdf.sunflower.p2pv2.eth.EthVersion
import org.tdf.sunflower.p2pv2.eth.message.EthMessageCodes
import org.tdf.sunflower.p2pv2.shh.ShhMessageCodes
import org.tdf.sunflower.p2pv2.swarm.BzzMessageCodes

class MessageCodesResolver(caps: List<Capability>) {
    private val offsets: MutableMap<String, Int> = mutableMapOf()

    init {
        initInternal(caps)
    }

    private fun initInternal(caps: List<Capability>) {
        val sorted = caps.toMutableList()
        sorted.sort()
        var offset: Int = P2pMessageCodes.USER.code + 1
        for (capability in caps) {
            if (capability.name == Capability.ETH) {
                ethOffset = offset
                val v = EthVersion.fromCode(capability.version)
                offset += EthMessageCodes.maxCode(v) + 1 // +1 is essential cause STATUS code starts from 0x0
            }
            if (capability.name == Capability.SHH) {
                shhOffset = offset
                offset += ShhMessageCodes.values().size
            }
            if (capability.name == Capability.BZZ) {
                bzzOffset = offset
                offset += BzzMessageCodes.values().size + 4
                // FIXME: for some reason Go left 4 codes between BZZ and ETH message codes
            }
        }
    }

    fun withP2pOffset(code: Int): Int {
        return withOffset(code, Capability.P2P)
    }

    fun withBzzOffset(code: Int): Int {
        return withOffset(code, Capability.BZZ)
    }

    fun withEthOffset(code: Int): Int {
        return withOffset(code, Capability.ETH)
    }

    fun withShhOffset(code: Int): Int {
        return withOffset(code, Capability.SHH)
    }

    fun withOffset(code: Int, cap: String): Int {
        val offset = getOffset(cap)
        return code + offset
    }

    fun resolveP2p(code: Int): Int {
        return resolve(code, Capability.P2P)
    }

    fun resolveBzz(code: Int): Int {
        return resolve(code, Capability.BZZ)
    }

    fun resolveEth(code: Int): Int {
        return resolve(code, Capability.ETH)
    }

    fun resolveShh(code: Int): Int {
        return resolve(code, Capability.SHH)
    }

    private fun resolve(code: Int, cap: String): Int {
        val offset = getOffset(cap)
        return code - offset
    }

    private fun getOffset(cap: String): Int {
        val offset = offsets[cap]
        return offset ?: 0
    }

    var bzzOffset: Int
        private set(v) {
            offsets[Capability.BZZ] = v
        }
        get() = offsets[Capability.BZZ] ?: 0

    var ethOffset: Int
        private set(v) {
            offsets[Capability.ETH] = v
        }
        get() = offsets[Capability.ETH] ?: 0

    var shhOffset: Int
        private set(v) {
            offsets[Capability.SHH] = v
        }
        get() = offsets[Capability.SHH] ?: 0

    var p2pOffset: Int
        private set(v) {
            offsets[Capability.P2P] = v
        }
        get() = offsets[Capability.P2P] ?: 0

}
