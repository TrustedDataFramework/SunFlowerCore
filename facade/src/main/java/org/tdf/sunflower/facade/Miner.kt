package org.tdf.sunflower.facade

import org.tdf.common.util.HexBytes
import org.tdf.sunflower.state.Address

interface Miner {
    fun start()
    fun stop()
    val minerAddress: HexBytes
        get() = Address.empty()

    companion object {
        val NONE: Miner = object : Miner {
            override fun start() {}
            override fun stop() {}
        }
    }
}