package org.tdf.sunflower

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.tdf.sunflower.facade.PropertiesWrapper
import org.tdf.sunflower.types.PropertyReader
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
class DatabaseConfig: Properties() {
    val rd = PropertyReader(PropertiesWrapper(this))
    val name: String get() = rd.getAsLowerCased("name")
    val maxOpenFiles: Int get() = rd.getAsInt("max-open-files")
    val directory: String get() = rd.getAsNonNull("directory")
    val reset: Boolean get() = rd.getAsBool("reset")
    val blockStore: String get() = rd.getAsNonNull("block-store")
}

@ConfigurationProperties(prefix = "sunflower")
@Component
class GlobalConfig : HashMap<String, Any>()


@ConfigurationProperties(prefix = "sunflower.p2p")
@Component
class PeerServerProperties : Properties()