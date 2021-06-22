package org.tdf.sunflower.pool

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.tdf.common.event.EventBus
import org.tdf.common.util.ByteUtil
import org.tdf.common.util.FixedDelayScheduler
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.AppConfig
import org.tdf.sunflower.TransactionPoolConfig
import org.tdf.sunflower.events.NewBestBlock
import org.tdf.sunflower.events.NewTransactionsCollected
import org.tdf.sunflower.facade.ConsensusEngine
import org.tdf.sunflower.facade.PendingTransactionValidator
import org.tdf.sunflower.facade.RepositoryService
import org.tdf.sunflower.facade.TransactionPool
import org.tdf.sunflower.state.Account
import org.tdf.sunflower.state.StateTrie
import org.tdf.sunflower.types.*
import org.tdf.sunflower.vm.Backend
import org.tdf.sunflower.vm.CallData.Companion.fromTransaction
import org.tdf.sunflower.vm.LockableBackend
import org.tdf.sunflower.vm.VMExecutor
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.function.Predicate
import javax.annotation.PostConstruct
import kotlin.concurrent.withLock
import kotlin.math.min

@Component
class TransactionPoolImpl(
    private val eventBus: EventBus,
    private val config: TransactionPoolConfig,
    private val repo: RepositoryService,
    private val trie: StateTrie<HexBytes, Account>,
    private val appCfg: AppConfig
) : TransactionPool {
    companion object {
        val log: Logger = LoggerFactory.getLogger("txPool")
    }

    // waited
    private val cache: TreeSet<TransactionInfo> = TreeSet()

    // hash -> info
    private val mCache: MutableMap<HexBytes, TransactionInfo> = mutableMapOf()
    private val lock: ReadWriteLock = ReentrantReadWriteLock()

    // dropped transactions
    private val dropped: Cache<HexBytes, Transaction> =
        CacheBuilder.newBuilder()
            .expireAfterWrite(config.expiredIn, TimeUnit.SECONDS)
            .build()

    private var validator: PendingTransactionValidator? = null

    // pending transaction
    private var parentHeader: Header? = null
    private val pending: MutableList<Transaction> = mutableListOf()
    private val pendingReceipts: MutableList<TransactionReceipt> = mutableListOf()
    private var current: Backend? = null
    private var gasUsed: Long = 0L
    private val executor = FixedDelayScheduler("TransactionPool", config.expiredIn)

    private fun resetInternal(best: Header) {
        parentHeader = best
        pending.clear()
        pendingReceipts.clear()
        current = trie.createBackend(parentHeader, null, false)
        gasUsed = 0
    }

    fun setEngine(engine: ConsensusEngine) {
        validator = engine.validator
        repo.getReader().use {
            resetInternal(it.bestHeader)
        }
    }

    private fun clearPending() {
        pending.clear()
        pendingReceipts.clear()
        parentHeader = null
        current = null
        gasUsed = 0L
    }

    private fun clear() {
        val now = System.currentTimeMillis()
        if (!lock.writeLock().tryLock(config.lockTimeout, TimeUnit.SECONDS)) {
            return
        }
        try {
            val lambda = Predicate { info: TransactionInfo ->
                val remove = (now - info.receivedAt) / 1000 > config.expiredIn
                if (remove) {
                    dropped.put(info.tx.hashHex, info.tx)
                }
                remove
            }
            cache.removeIf(lambda)
            mCache.values.removeIf(lambda)
        } finally {
            lock.writeLock().unlock()
        }
    }

    override fun collect(transactions: Collection<Transaction>): Map<HexBytes, String> {
        val errors: MutableMap<HexBytes, String> = mutableMapOf()
        lock.writeLock().lock()
        try {
            val newCollected: MutableList<Transaction> = mutableListOf()
            for (transaction in transactions) {
                if (transaction.gasPriceAsU256 < appCfg.vmGasPrice) throw RuntimeException("transaction pool: gas price of tx less than vm gas price ${appCfg.vmGasPrice}")
                val info = TransactionInfo(System.currentTimeMillis(), transaction)
                if (cache.contains(info) || dropped.asMap().containsKey(transaction.hashHex)) continue
                try {
                    transaction.verify()
                } catch (e: Exception) {
                    log.error(e.message)
                    continue
                }
                val res = validator!!.validate(parentHeader, transaction)
                if (res.isSuccess) {
                    cache.add(info)
                    mCache[info.tx.hashHex] = info
                    newCollected.add(transaction)
                } else {
                    log.error(res.reason)
                    errors[transaction.hashHex] = res.reason
                }
            }
            if (newCollected.isNotEmpty())
                eventBus.publish(NewTransactionsCollected(newCollected))
            execute(errors)
        } finally {
            lock.writeLock().unlock()
        }
        return errors
    }

    private fun execute(errors: MutableMap<HexBytes, String>) {
        val it = cache.iterator()
        if (parentHeader == null) return
        while (it.hasNext()) {
            val t = it.next().tx
            val prevNonce = current!!.getNonce(t.senderHex)
            if (t.nonceAsLong < prevNonce) {
                it.remove()
                errors[t.hashHex] = "nonce is too small"
                mCache.remove(t.hashHex)
                dropped.put(t.hashHex, t)
                continue
            }
            if (t.nonceAsLong != prevNonce) {
                continue
            }

            val blockGasLimit = appCfg.blockGasLimit
            if (gasUsed >= blockGasLimit)
                return

            // try to execute
            try {
                val child = current!!.createChild()
                val callData = fromTransaction(t, false)
                val vmExecutor = VMExecutor(
                    child, callData,
                    min(t.gasLimitAsU256.toLong(), blockGasLimit)
                )

                val res = vmExecutor.execute()


                // execute successfully
                if (gasUsed + res.gasUsed > blockGasLimit)
                    return
                val receipt = TransactionReceipt(
                    ByteUtil.longToBytesNoLeadZeroes(gasUsed + res.gasUsed),
                    Bloom(), emptyList()
                )
                receipt.setGasUsed(res.gasUsed)
                receipt.executionResult = res.executionResult
                receipt.transaction = t
                pending.add(t)
                pendingReceipts.add(receipt)
                this.current = child
                this.gasUsed += res.gasUsed
            } catch (e: Exception) {
                errors[t.hashHex] = e.message ?: ""
            } finally {
                it.remove()
                mCache.remove(t.hashHex)
            }
        }
    }

    override fun pop(parentHeader: Header): PendingData {
        lock.writeLock().lock()
        try {
            if (this.parentHeader != null && parentHeader.hash != this.parentHeader!!.hash) {
                clearPending()
                log.warn("parent header is not equal, drop pending")
            }

            val r = PendingData(
                pending.toMutableList(),
                pendingReceipts.toMutableList(),
                current
            )
            clearPending()
            return r
        } finally {
            lock.writeLock().unlock()
        }
    }

    override fun reset(parent: Header) {
        if (!lock.writeLock().tryLock())
            return
        try {
            resetInternal(parent)
        } finally {
            lock.writeLock().unlock()
        }

    }

    override fun current(): Backend {
        lock.readLock().withLock {
            repo.getReader().use {
                return if (current != null) {
                    this.lock.readLock().lock()
                    val child = current!!.createChild()
                    val b = LockableBackend(child, this.lock)
                    b
                } else {
                    val best = it.bestHeader
                    trie.createBackend(best, best.stateRoot, null, false)
                }
            }
        }
    }

    private fun onNewBestBlock(event: NewBestBlock) {
        if (!lock.writeLock().tryLock()) {
            return
        }
        try {
            if (event.block.stateRoot == this.parentHeader?.stateRoot) return
            resetInternal(event.block.header)
        } finally {
            lock.writeLock().unlock()
        }
    }

    internal class TransactionInfo(val receivedAt: Long, val tx: Transaction) : Comparable<TransactionInfo> {
        override fun compareTo(other: TransactionInfo): Int {
            var cmp = tx.senderHex.compareTo(other.tx.senderHex)
            if (cmp != 0) return cmp
            cmp = tx.nonceAsLong.compareTo(other.tx.nonceAsLong)
            if (cmp != 0) return cmp
            cmp = -tx.gasPriceAsU256.compareTo(other.tx.gasPriceAsU256)
            return if (cmp != 0) cmp else tx.hashHex.compareTo(other.tx.hashHex)
        }
    }

    @PostConstruct
    fun initialize() {
        eventBus.subscribe(NewBestBlock::class.java) { event: NewBestBlock -> onNewBestBlock(event) }
        executor.delay{
            try {
                clear()
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }
}