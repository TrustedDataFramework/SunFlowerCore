package org.tdf.sunflower.vm

import org.tdf.lotusvm.runtime.StackAllocator

class SimpleStackResource(val p: StackAllocator): StackAllocator by p, StackResource{
    override fun close() {

    }
}