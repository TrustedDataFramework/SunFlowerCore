package org.tdf.sunflower.vm

import org.tdf.lotusvm.runtime.StackAllocator
import java.util.concurrent.locks.Lock

class LockedStackResource(val p: StackAllocator, val lock: Lock): StackAllocator by p, StackResource{
    override fun close() {
        p.clear()
        lock.unlock()
    }
}