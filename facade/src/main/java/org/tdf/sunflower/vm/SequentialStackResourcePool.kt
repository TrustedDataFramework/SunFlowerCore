package org.tdf.sunflower.vm

import org.tdf.lotusvm.runtime.LimitedStackAllocator
import java.lang.RuntimeException

class SequentialStackResourcePool(val maxSize: Int) : StackResourcePool {
    private val resources: Array<RcStackResource?> = arrayOfNulls(maxSize)

    override fun tryGet(): StackResource {
        for(i in (0..resources.size)) {
            resources[i] = resources[i] ?:
                RcStackResource(LimitedStackAllocator(VMExecutor.MAX_STACK_SIZE, VMExecutor.MAX_FRAMES, VMExecutor.MAX_LABELS))
            if(resources[i]!!.refers() > 0)
                continue
            return resources[i]!!
        }
        throw RuntimeException("resource pool overflow")
    }
}