package org.tdf.sunflower.facade;

import java.util.concurrent.locks.ReentrantReadWriteLock

interface IRepositoryService {
    fun getReader(): RepositoryReader
    fun getWriter(): RepositoryWriter
}

class RepositoryService(private val proxy: RepositoryWriter) : IRepositoryService {
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