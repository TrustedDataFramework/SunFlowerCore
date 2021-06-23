package org.tdf.sunflower.facade;

import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantReadWriteLock

interface RepositoryService {
    val reader: RepositoryReader
    val writer: RepositoryWriter
}

class LogLock(private val lock: Lock, private val name: String = "") : Lock {
    companion object {
        val log = LoggerFactory.getLogger("lock")
    }

    override fun lock() {
        log.info("$name lock required by thread ${Thread.currentThread().name}")
        lock.lock()
        log.info("$name lock successfully by thread ${Thread.currentThread().name}")

    }

    override fun lockInterruptibly() {
        log.info("$name lock interruptibly by thread ${Thread.currentThread().name}")
        lock.lockInterruptibly()
    }

    override fun tryLock(): Boolean {
        val r = lock.tryLock()
        log.info("$name try lock by thread ${Thread.currentThread().name} result = $r")
        return r
    }

    override fun tryLock(time: Long, unit: TimeUnit): Boolean {
        val r = lock.tryLock(time, unit)
        log.info("$name try lock by thread ${Thread.currentThread().name} result = $r")
        return r
    }

    override fun unlock() {
        log.info("$name unlock required by thread ${Thread.currentThread().name}")
        lock.unlock()
        log.info("$name unlock successfully by thread ${Thread.currentThread().name}")
    }

    override fun newCondition(): Condition {
        val r = lock.newCondition()
        log.info("$name newCondition by thread ${Thread.currentThread().name}")
        return r
    }

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