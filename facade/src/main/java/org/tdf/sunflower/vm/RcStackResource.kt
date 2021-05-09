package org.tdf.sunflower.vm

import org.tdf.lotusvm.runtime.StackProvider

class RcStackResource (val p: StackProvider, private var refers: Int = 0): StackProvider by p, StackResource{
    override fun close() {
        refers --
    }

    fun refer() {
        refers++
    }

    fun refers(): Int{
        return refers
    }
}