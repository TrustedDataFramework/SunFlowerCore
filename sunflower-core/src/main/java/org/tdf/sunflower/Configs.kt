package org.tdf.sunflower

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.util.*

object ApplicationConstants {
    const val CONSENSUS_POA = "poa"
    const val CONSENSUS_NONE = "none"
    const val CONSENSUS_POW = "pow"
    const val CONSENSUS_POS = "pos"
    const val CONSENSUS_VRF = "vrf"
    const val MAX_SHUTDOWN_WAITING: Long = 5
}


@ConfigurationProperties(prefix = "sunflower.consensus")
@Component
object ConsensusProperties : Properties() {
    const val CONSENSUS_NAME = "name"
}

@ConfigurationProperties(prefix = "sunflower.database")
@Component
data class DatabaseConfig(
    var name: String = "",
    var maxOpenFiles: Int = 0,
    var directory: String = "",
    var isReset: Boolean = false,
    var blockStore: String = "",
)

@ConfigurationProperties(prefix = "sunflower")
@Component
class GlobalConfig : HashMap<String, Any>()


@ConfigurationProperties(prefix = "sunflower.p2p")
@Component
class PeerServerProperties : Properties()