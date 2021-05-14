package org.tdf.sunflower.facade;

import java.util.concurrent.locks.ReentrantReadWriteLock

interface RepositoryService {
    fun getReader(): RepositoryReader
    fun getWriter(): RepositoryWriter
}

class RepositoryServiceImpl(private val proxy: RepositoryWriter) : RepositoryService {
    val lock = ReentrantReadWriteLock()

    override fun getReader(): RepositoryReader {
        lock.readLock().lock()
        return LockedRepositoryReader(proxy, lock.readLock())
    }

    override fun getWriter(): RepositoryWriter {
        lock.writeLock().lock()
        return LockedRepositoryWriter(proxy, lock.writeLock())
    }
}