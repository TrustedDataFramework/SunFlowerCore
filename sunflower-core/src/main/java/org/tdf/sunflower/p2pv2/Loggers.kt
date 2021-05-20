package org.tdf.sunflower.p2pv2

import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface Loggers {
    companion object {
        val net: Logger = LoggerFactory.getLogger("net")
    }

    val net: Logger
        get() = Loggers.net
}