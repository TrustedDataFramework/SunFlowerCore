package org.tdf.sunflower.p2pv2.message

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.tdf.sunflower.AppConfig
import org.tdf.sunflower.p2pv2.client.Capability
import org.tdf.sunflower.p2pv2.eth.EthVersion
import org.tdf.sunflower.p2pv2.p2p.HelloMessage
import java.util.*

@Component
class StaticMessages @Autowired constructor(private val cfg: AppConfig) {
    private val allCaps: SortedSet<Capability>

    init {
        val allCaps: SortedSet<Capability> = TreeSet()
        for (v in EthVersion.supported()) {
            allCaps.add(Capability(Capability.ETH, v.code))
        }
        this.allCaps = allCaps
    }

    /**
     * Gets the capabilities listed in 'peer.capabilities' config property
     * sorted by their names.
     */
    val configCapabilities: List<Capability>
        get() {
            val ret: MutableList<Capability> = ArrayList()
            val caps: MutableList<String> = cfg.peerCapabilities().toMutableList()
            for (capability in this.allCaps) {
                if (caps.contains(capability.name)) {
                    ret.add(capability)
                }
            }
            return ret
        }

    fun createHelloMessage(peerId: String, listenPort: Int = cfg.listenPort): HelloMessage {
        val p2pVersion = cfg.defaultP2PVersion
        return HelloMessage(
            p2pVersion, "",
            configCapabilities.toTypedArray(), listenPort, peerId
        )
    }
}