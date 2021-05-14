package org.tdf.sunflower.vm

import java.util.concurrent.locks.ReadWriteLock


class LockableBackend(
    private val backend: Backend,
    private val lock: ReadWriteLock
) : Backend by backend {
    override fun close() {
        lock.readLock().unlock()
    }
}