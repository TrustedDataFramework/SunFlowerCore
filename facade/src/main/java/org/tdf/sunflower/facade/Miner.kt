package org.tdf.sunflower.facade

import org.tdf.sunflower.state.AddrUtil
import org.tdf.sunflower.types.Address

interface Miner {
    fun start()
    fun stop()
    val address: Address
        get() = AddrUtil.empty()

    companion object {
        val NONE: Miner = object : Miner {
            override fun start() {}
            override fun stop() {}
        }
    }
}