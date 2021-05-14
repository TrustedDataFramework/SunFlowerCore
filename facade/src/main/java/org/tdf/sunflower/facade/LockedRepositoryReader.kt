package org.tdf.sunflower.facade

import org.tdf.sunflower.facade.RepositoryReader
import java.io.Closeable
import java.util.concurrent.locks.Lock

class LockedRepositoryReader(private val proxy: RepositoryReader, val lock: Lock): RepositoryReader by proxy, Closeable{
    override fun close() {
        lock.unlock()
    }
}