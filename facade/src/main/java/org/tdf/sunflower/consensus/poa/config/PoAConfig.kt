package org.tdf.sunflower.consensus.poa.config

import org.tdf.common.crypto.ECKey
import org.tdf.sunflower.facade.PropertyLike
import org.tdf.sunflower.types.ConsensusConfig
import org.tdf.common.util.hex
import org.tdf.sunflower.types.Address

class PoAConfig(properties: PropertyLike) : ConsensusConfig(properties) {
    val threadId: Int
        get() = reader.getAsInt("thread-id", 0)

    val farmBaseAdmin: Address?
        get() = reader.getAsAddress("farm-base-admin")

    val gatewayNode: String
        get() = reader.getAsNonNull("gateway-node")

    val minerCoinBase: Address?
        get() {
            val key = privateKey?.bytes?.let { ECKey.fromPrivate(it) }
            return key?.address?.hex()
        }

    val controlled: Boolean
        get() = threadId != 0

    companion object {
        @JvmStatic
        fun from(c: ConsensusConfig): PoAConfig {
            return PoAConfig(c.properties)
        }
    }
}