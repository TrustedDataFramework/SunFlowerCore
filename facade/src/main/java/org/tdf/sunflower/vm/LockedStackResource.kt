package org.tdf.sunflower.vm

import org.tdf.lotusvm.runtime.StackProvider
import java.util.concurrent.locks.Lock

class LockedStackResource(val p: StackProvider, val lock: Lock): StackProvider by p, StackResource{
    override fun close() {
        lock.unlock()
    }
}