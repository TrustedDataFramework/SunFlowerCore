package org.tdf.sunflower.service

import org.springframework.stereotype.Service
import org.tdf.lotusvm.runtime.LimitedStackAllocator
import org.tdf.lotusvm.runtime.StackAllocator
import org.tdf.sunflower.vm.LockedStackResource
import org.tdf.sunflower.vm.StackResourcePool
import org.tdf.sunflower.vm.VMExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

@Service
class StackResourcePoolImpl : StackResourcePool {
    private val resources = arrayOfNulls<StackAllocator>(MAX_POOL_SIZE)
    private val locks: List<Lock>

    init {
        val locks = mutableListOf<Lock>()
        for (i in (0 until MAX_POOL_SIZE)) {
            locks.add(ReentrantLock())
        }
        this.locks = locks
    }

    override fun tryGet(): LockedStackResource {
        for (i in (locks.indices)) {
            if (!locks[i].tryLock(
                    1, if (i == MAX_POOL_SIZE - 1) {
                        TimeUnit.SECONDS
                    } else {
                        TimeUnit.MILLISECONDS
                    }
                )
            )
                continue
            if (resources[i] == null)
                resources[i] =
                    LimitedStackAllocator(
                        VMExecutor.MAX_STACK_SIZE,
                        VMExecutor.MAX_FRAMES,
                        VMExecutor.MAX_LABELS
                    )
            return LockedStackResource(resources[i]!!, locks[i])
        }
        throw RuntimeException("busy...")
    }

    companion object {
        const val MAX_POOL_SIZE = 16
    }
}