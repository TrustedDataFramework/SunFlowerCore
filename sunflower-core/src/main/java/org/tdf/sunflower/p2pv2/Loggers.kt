package org.tdf.sunflower.p2pv2

import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface Loggers {
    companion object {
        val net: Logger = LoggerFactory.getLogger("net")
        val wire: Logger = LoggerFactory.getLogger("wire")
        val dev: Logger = LoggerFactory.getLogger("dev")
    }

    val net: Logger
        get() = Loggers.net
    val wire: Logger
        get() = Loggers.wire
    val dev: Logger
        get() = Loggers.dev
}