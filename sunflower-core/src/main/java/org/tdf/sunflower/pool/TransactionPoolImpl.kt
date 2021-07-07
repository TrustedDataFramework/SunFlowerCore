package org.tdf.sunflower.pool

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.tdf.common.event.EventBus
import org.tdf.common.util.FixedDelayScheduler
import org.tdf.common.util.HexBytes
import org.tdf.common.util.LogLock
import org.tdf.common.util.bytes
import org.tdf.sunflower.AppConfig
import org.tdf.sunflower.TransactionPoolConfig
import org.tdf.sunflower.events.NewBestBlock
import org.tdf.sunflower.events.NewTransactionsCollected
import org.tdf.sunflower.facade.*
import org.tdf.sunflower.state.Account
import org.tdf.sunflower.state.StateTrie
import org.tdf.sunflower.types.*
import org.tdf.sunflower.vm.Backend
import org.tdf.sunflower.vm.CallContext
import org.tdf.sunflower.vm.CallData
import org.tdf.sunflower.vm.VMExecutor
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
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

    // waiting
    private val cache: TreeSet<TransactionInfo> = TreeSet()

    private val lock: ReadWriteLock = ReentrantReadWriteLock()
    private val readLock = LogLock(lock.readLock(), "tp-r")
    private val writeLock = LogLock(lock.writeLock(), "tp-w")

    // dropped transactions
    private val dropped: Cache<HexBytes, String> =
        CacheBuilder.newBuilder()
            .expireAfterWrite(config.expiredIn, TimeUnit.SECONDS)
            .build()

    private lateinit var validator: PendingTransactionValidator

    // pending transaction
    private var parentHeader: Header? = null
    private val pending: MutableList<Transaction> = mutableListOf()
    private val pendingReceipts: MutableList<TransactionReceipt> = mutableListOf()

    private var current: Backend? = null
    private var currentBloom: Bloom = Bloom()
    private var gasUsed: Long = 0L

    private val clearScheduler = FixedDelayScheduler("txPool-clear", config.expiredIn)
    private val executeScheduler = FixedDelayScheduler("txPool-execute", 1)

    private fun resetInternal(best: Header) {
        parentHeader = best
        pending.clear()
        pendingReceipts.clear()
        current = trie.createBackend(best)
        gasUsed = 0
        currentBloom = Bloom()
    }

    fun setEngine(engine: ConsensusEngine) {
        validator = engine.validator
        repo.reader.use {
            resetInternal(it.bestHeader)
        }
    }

    private fun clearPending() {
        pending.clear()
        pendingReceipts.clear()
        parentHeader = null
        current = null
        gasUsed = 0L
        currentBloom = Bloom()
    }

    /**
     *
     */
    private fun clear() {
        val now = System.currentTimeMillis()
        if (!writeLock.tryLock(config.lockTimeout, TimeUnit.SECONDS)) {
            return
        }
        try {
            cache.removeIf { info: TransactionInfo ->
                val remove = (now - info.receivedAt) / 1000 > config.expiredIn
                if (remove) {
                    dropped.put(info.tx.hash, "transaction timeout")
                }
                remove
            }
        } finally {
            writeLock.unlock()
        }
    }

    override fun collect(rd: RepositoryReader, transactions: Collection<Transaction>): Map<HexBytes, String> {
        val errors: MutableMap<HexBytes, String> = mutableMapOf()
        writeLock.lock()
        try {
            val newCollected: MutableList<Transaction> = mutableListOf()
            for (tx in transactions) {
                if (tx.gasPrice < appCfg.vmGasPrice) {
                    errors[tx.hash] = "transaction pool: gas price of tx less than vm gas price ${appCfg.vmGasPrice}"
                    continue
                }
                val err = dropped.asMap()[tx.hash]
                if (err != null) {
                    errors[tx.hash] = err
                    continue
                }

                val info = TransactionInfo(System.currentTimeMillis(), tx)
                if (cache.contains(info)) continue

                try {
                    tx.validate()
                } catch (e: Exception) {
                    errors[tx.hash] = e.message ?: "validate transaction failed"
                    continue
                }
                if (!tx.verifySig) {
                    errors[tx.hash] = "validate signature failed"
                    continue
                }

                val res = validator.validate(rd, parentHeader!!, tx)
                if (res.success) {
                    cache.add(info)
                    newCollected.add(tx)
                } else {
                    errors[tx.hash] = res.reason
                }
            }
            execute(rd)
            transactions.forEach { t ->
                dropped.asMap()[t.hash]?.let { errors[t.hash] = it }
            }
            if (newCollected.isNotEmpty())
                eventBus.publish(NewTransactionsCollected(newCollected))
        } finally {
            writeLock.unlock()
        }
        return errors
    }

    private fun execute(rd: RepositoryReader) {
        val it = cache.iterator()
        if (parentHeader == null) return
        while (it.hasNext()) {
            val t = it.next().tx
            val prevNonce = current!!.getNonce(t.sender)
            if (t.nonce < prevNonce) {
                it.remove()
                dropped.asMap()[t.hash] = "nonce is too small"
                continue
            }

            if (t.nonce != prevNonce) {
                continue
            }

            val blockGasLimit = appCfg.blockGasLimit

            // try to execute
            try {
                val child = current!!.createChild()
                val ctx = CallContext.fromTx(t)
                val callData = CallData.fromTx(t, false)
                val vmExecutor = VMExecutor(
                    rd,
                    child, ctx, callData,
                    min(t.gasLimit, blockGasLimit)
                )

                val res = vmExecutor.execute()

                if (res.gasUsed > blockGasLimit) {
                    it.remove()
                    dropped.asMap()[t.hash] = "gas overflows block gas limit"
                    continue
                }

                // execute successfully
                if (gasUsed + res.gasUsed > blockGasLimit) {
                    continue
                }

                val receipt = TransactionReceipt(
                    (gasUsed + res.gasUsed).bytes(),
                    Bloom(), emptyList()
                )

                receipt.setGasUsed(res.gasUsed)
                receipt.executionResult = res.executionResult.bytes
                receipt.transaction = t
                receipt.logInfoList = res.logs

                currentBloom.or(receipt.bloomFilter)
                pending.add(t)
                pendingReceipts.add(receipt)
                this.current = child
                this.gasUsed += res.gasUsed
            } catch (e: Exception) {
                dropped.asMap()[t.hash] = e.message ?: "execute tx error"
                it.remove()
            }
        }
    }

    override fun pop(parentHeader: Header): PendingData {
        writeLock.lock()
        try {
            if (this.parentHeader != null && parentHeader.hash != this.parentHeader!!.hash) {
                clearPending()
                log.warn("parent header is not equal, drop pending")
            }

            val r = PendingData(
                pending.toMutableList(),
                pendingReceipts.toMutableList(),
                current,
                currentBloom
            )
            clearPending()
            return r
        } finally {
            writeLock.unlock()
        }
    }

    override fun reset(parent: Header) {
        if (!writeLock.tryLock())
            return
        try {
            resetInternal(parent)
        } finally {
            writeLock.unlock()
        }

    }

    override fun current(): Backend? {
        readLock.withLock {
            return current?.createChild()
        }
    }

    private fun onNewBestBlock(event: NewBestBlock) {
        if (!writeLock.tryLock()) {
            return
        }
        try {
            if (event.block.stateRoot == this.parentHeader?.stateRoot) return
            resetInternal(event.block.header)
        } finally {
            writeLock.unlock()
        }
    }

    internal class TransactionInfo(val receivedAt: Long, val tx: Transaction) : Comparable<TransactionInfo> {
        override fun compareTo(other: TransactionInfo): Int {
            var cmp = -tx.gasPrice.compareTo(other.tx.gasPrice)
            if (cmp != 0) return cmp
            cmp = tx.sender.compareTo(other.tx.sender)
            if (cmp != 0) return cmp
            cmp = tx.nonce.compareTo(other.tx.nonce)
            if (cmp != 0) return cmp
            return tx.hash.compareTo(other.tx.hash)
        }
    }

    @PostConstruct
    fun initialize() {
        eventBus.subscribe(NewBestBlock::class.java) { event: NewBestBlock -> onNewBestBlock(event) }
        clearScheduler.delay {
            try {
                clear()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        executeScheduler.delay {
            val r = repo.reader(config.lockTimeout, TimeUnit.SECONDS) ?: return@delay
            r.use {
                if (!writeLock.tryLock(config.lockTimeout, TimeUnit.SECONDS))
                    return@delay
                try {
                    execute(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    writeLock.unlock()
                }
            }

        }
    }
}