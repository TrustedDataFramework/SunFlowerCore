package org.tdf.sunflower.vm

import org.tdf.lotusvm.runtime.StackProvider

class SimpleStackResource(val p: StackProvider): StackProvider by p, StackResource{
    override fun close() {

    }

}