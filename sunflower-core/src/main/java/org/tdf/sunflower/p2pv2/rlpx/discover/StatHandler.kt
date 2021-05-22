package org.tdf.sunflower.p2pv2.rlpx.discover

import java.util.concurrent.atomic.AtomicLong

class StatHandler {
    val count = AtomicLong(0L)

    fun add() {
        count.incrementAndGet()
    }

    fun add(delta: Long) {
        count.addAndGet(delta)
    }

    fun get() {
        count.get()
    }
}