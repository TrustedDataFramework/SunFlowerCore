package org.tdf.sunflower.vm

import org.tdf.lotusvm.runtime.StackAllocator

class RcStackResource (val p: StackAllocator, private var refers: Int = 0): StackAllocator by p, StackResource{
    override fun close() {
        refers --
        this.p.clear()
    }

    fun refer() {
        refers++
    }

    fun refers(): Int{
        return refers
    }
}