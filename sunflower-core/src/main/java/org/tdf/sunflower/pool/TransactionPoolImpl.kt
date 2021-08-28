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
import kotlin.concurrent.withLock
import kotlin.math.min

@Component
class TransactionPoolImpl(
    private val eventBus: EventBus,
    private val config: TransactionPoolConfig,
    private val repo: RepositoryService,
    private val trie: StateTrie<HexBytes, Account>,
    private val appCfg: AppConfig,
    private val conCfg: ConsensusConfig
) : TransactionPool {
    companion object {
        val log: Logger = LoggerFactory.getLogger("txPool")
    }

    // waiting, sorted by gasPrice
    private val waiting: TreeSet<WaitingInfo> = TreeSet()

    private val lock: ReadWriteLock = ReentrantReadWriteLock()
    private val readLock = LogLock(lock.readLock(), "tp-r")
    private val writeLock = LogLock(lock.writeLock(), "tp-w")

    // dropped transactions
    val cache: Cache<HexBytes, Dropped> =
        CacheBuilder.newBuilder()
            .expireAfterWrite(config.expiredIn, TimeUnit.SECONDS)
            .build()

    override val dropped: MutableMap<HexBytes, Dropped> get() = cache.asMap()

    private var parentHeader: Header? = null

    // pending transaction
    private val pending: MutableList<WaitingInfo> = mutableListOf()

    // pending gas used
    private var pendingGas: Long = 0

    // pending state
    private var current: Backend? = null

    // pending timestamp
    private var timestamp: Long = 0

    private val clearScheduler = FixedDelayScheduler("txPool-clear", config.expiredIn)
    private val executeScheduler = FixedDelayScheduler("txPool-execute", 1)

    private fun resetInternal(best: Header) {
        parentHeader = best
        timestamp = if (best.height == 0L) {
            System.currentTimeMillis() / 1000
        } else {
            best.createdAt + conCfg.blockInterval
        }
        pending.clear()
        current = trie.createBackend(best)
    }

    fun init() {
        repo.reader.use {
            resetInternal(it.bestHeader)
        }
    }

    private fun clearPending() {
        pending.clear()
        pendingGas = 0
        parentHeader = null
        current = null
    }

    /**
     * clear waiting transactions
     */
    private fun clear() {
        val now = System.currentTimeMillis()
        if (!writeLock.tryLock(config.lockTimeout, TimeUnit.SECONDS)) {
            return
        }
        try {
            waiting.removeIf {
                val remove = (now - it.receivedAt) / 1000 > config.expiredIn
                if (remove) {
                    dropped[it.tx.hash] = Dropped(it.tx, "transaction timeout")
                }
                remove
            }
        } finally {
            writeLock.unlock()
        }
    }

    override fun collect(rd: RepositoryReader, transactions: Collection<Transaction>): Map<HexBytes, String> {
        val errors: MutableMap<HexBytes, String> = mutableMapOf()

        writeLock.withLock {
            val newCollected: MutableList<Transaction> = mutableListOf()
            for (tx in transactions) {
                println(tx)
                if (tx.gasPrice < appCfg.vmGasPrice) {
                    errors[tx.hash] = "transaction pool: gas price of tx less than vm gas price ${appCfg.vmGasPrice}"
                    continue
                }

                if (pending.any { it.tx.hash == tx.hash } || rd.containsTransaction(tx.hash))
                    continue

                val drop = dropped[tx.hash]
                if (drop != null) {
                    errors[tx.hash] = drop.err
                    continue
                }

                val info = WaitingInfo(System.currentTimeMillis(), tx)
                if (waiting.contains(info)) continue

                try {
                    tx.validate()
                } catch (e: Exception) {
                    errors[tx.hash] = e.message ?: "validate transaction failed"
                    continue
                }
                if (tx.vrs == null || !tx.verifySig) {
                    errors[tx.hash] = "validate signature failed"
                    continue
                }

                if (tx.chainId != appCfg.chainId) {
                    errors[tx.hash] = "invalid chainId ${tx.chainId}"
                    continue
                }

                waiting.add(info)
                newCollected.add(tx)
            }

            // try to move transactions from waiting to pending
            // pendingGas <= blockGasLimit
            current?.let {
                val ex = Execute(waiting.iterator(), it, rd, timestamp, pendingGas)
                val d = execute(ex)
                pending.addAll(d.first)
                this.current = ex.backend
            }

            transactions.forEach { t ->
                this.dropped[t.hash]?.let { errors[t.hash] = it.second }
            }
            if (newCollected.isNotEmpty())
                eventBus.publish(NewTransactionsCollected(newCollected))
            return errors
        }
    }

    internal data class Execute(
        val it: MutableIterator<WaitingInfo>,
        var backend: Backend,
        var rd: RepositoryReader,
        var timestamp: Long,
        var gasUsed: Long
    )

    // remove from waiting if success, keep unchanged if nonce too big, moved to drop if nonce too small or failed
    private fun execute(ex: Execute): Pair<List<WaitingInfo>, List<TransactionReceipt>> {
        val it = ex.it
        val pending = mutableListOf<WaitingInfo>()
        val receipts = mutableListOf<TransactionReceipt>()

        while (it.hasNext()) {
            val n = it.next()
            val t = n.tx
            val prevNonce = ex.backend.getNonce(t.sender)
            if (t.nonce < prevNonce) {
                it.remove()
                this.dropped[t.hash] = Pair(t, "nonce is too small")
                continue
            }

            if (t.nonce != prevNonce) {
                continue
            }

            val blockGasLimit = appCfg.blockGasLimit

            // try to execute
            try {
                val child = ex.backend.createChild()
                val ctx = CallContext.fromTx(tx = t, timestamp = ex.timestamp)
                val callData = CallData.fromTx(t)
                val vmExecutor = VMExecutor.create(
                    ex.rd,
                    child, ctx, callData,
                    min(t.gasLimit, blockGasLimit)
                )

                val res = vmExecutor.execute()

                if (res.gasUsed > blockGasLimit) {
                    it.remove()
                    this.dropped[t.hash] = Pair(t, "gas overflows block gas limit")
                    continue
                }

                // execute successfully
                if (ex.gasUsed + res.gasUsed > blockGasLimit) {
                    continue
                }

                val receipt = TransactionReceipt(
                    cumulativeGas = ex.gasUsed + res.gasUsed,
                    logInfoList = res.logs,
                    gasUsed = res.gasUsed,
                    result = res.executionResult
                )

                pending.add(n)
                receipts.add(receipt)
                ex.backend = child
                ex.gasUsed += res.gasUsed
                it.remove()
            } catch (e: Exception) {
                e.printStackTrace()
                this.dropped[t.hash] = Pair(t, e.message ?: "execute tx error")
                it.remove()
            }
        }
        return Pair(pending, receipts)
    }

    override fun pop(rd: RepositoryReader, parentHeader: Header, timestamp: Long): PendingData {
        writeLock.withLock {
            val ex = Execute(
                pending.iterator(),
                trie.createBackend(parentHeader),
                rd,
                timestamp,
                0L
            )

            val r = execute(ex)
            clearPending()
            return PendingData(r.first.map { it.tx }, r.second, ex.backend)
        }
    }

    override fun reset(parent: Header) {
        writeLock.withLock {
            resetInternal(parent)
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

    internal class WaitingInfo(val receivedAt: Long, val tx: Transaction) : Comparable<WaitingInfo> {
        override fun compareTo(other: WaitingInfo): Int {
            var cmp = -tx.gasPrice.compareTo(other.tx.gasPrice)
            if (cmp != 0) return cmp
            cmp = tx.sender.compareTo(other.tx.sender)
            if (cmp != 0) return cmp
            cmp = tx.nonce.compareTo(other.tx.nonce)
            if (cmp != 0) return cmp
            return tx.hash.compareTo(other.tx.hash)
        }
    }

    fun initialize() {
        eventBus.subscribe(NewBestBlock::class.java) { onNewBestBlock(it) }
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
                    val c = current ?: return@delay
                    val ex = Execute(waiting.iterator(), c, it, timestamp, pendingGas)
                    val d = execute(ex)
                    pending.addAll(d.first)
                    this.current = ex.backend
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    writeLock.unlock()
                }
            }

        }
    }
}