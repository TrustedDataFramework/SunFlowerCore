package org.tdf.sunflower

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.tdf.common.util.HexBytes

@ConfigurationProperties(prefix = "sunflower.sync")
@Component
data class SyncConfig (
    var heartRate: Long = 0,
    var blockWriteRate: Long = 0,
    var maxPendingBlocks: Long = 0,
    var maxBlocksTransfer: Int = 0,
    var pruneHash: String = "",
    var fastSyncHeight: Long = 0,
    var lockTimeout: Long = 0,
    var fastSyncHash: String = "",
    var maxAccountsTransfer: Int = 0,
    var rateLimits: Map<String, Double> = mutableMapOf()
) {
    val fastSyncHashHex: HexBytes
        get() = HexBytes.fromHex(fastSyncHash)
}