package org.tdf.sunflower.facade;

import org.tdf.common.util.LogLock
import java.util.concurrent.locks.ReentrantReadWriteLock

interface RepositoryService {
    val reader: RepositoryReader
    val writer: RepositoryWriter
}

class RepositoryServiceImpl(private val proxy: RepositoryWriter) : RepositoryService {
    val lock = ReentrantReadWriteLock()
    private val readLock = LogLock(lock.readLock(), "repo-r")
    private val writeLock = LogLock(lock.writeLock(), "repo-w")

    override val reader: RepositoryReader
        get() {
            readLock.lock()
            return LockedRepositoryReader(proxy, readLock)
        }

    override val writer: RepositoryWriter
        get() {
            writeLock.lock()
            return LockedRepositoryWriter(proxy, writeLock)
        }
}