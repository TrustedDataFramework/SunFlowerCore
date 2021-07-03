package org.tdf.sunflower

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties(prefix = "sunflower.database")
@Component
data class DatabaseConfig(
    var name: String = "",
    var maxOpenFiles: Int = 0,
    var directory: String = "",
    var isReset: Boolean = false,
    var blockStore: String = "",
)