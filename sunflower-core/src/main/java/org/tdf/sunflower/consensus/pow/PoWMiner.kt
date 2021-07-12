package org.tdf.sunflower.consensus.pow

import org.slf4j.LoggerFactory
import org.tdf.common.types.Constants.NONCE_SIZE
import org.tdf.common.types.Uint256
import org.tdf.sunflower.facade.TransactionPool
import org.tdf.sunflower.consensus.AbstractMiner
import org.tdf.common.util.hex
import org.tdf.sunflower.ApplicationConstants.MAX_SHUTDOWN_WAITING
import java.lang.Runnable
import java.lang.InterruptedException
import org.tdf.sunflower.events.NewBlockMined
import org.tdf.sunflower.events.NewBestBlock
import org.tdf.sunflower.types.*
import java.lang.Exception
import java.util.*
import java.util.concurrent.*

class PoWMiner(
    private val config: ConsensusConfig,
    private val transactionPool: TransactionPool,
    private val poW: PoW
) : AbstractMiner(poW.accountTrie, poW.eventBus, config, poW.transactionPool) {
    private val threadPool = Executors.newWorkStealingPool() as ForkJoinPool
    private lateinit var minerExecutor: ScheduledExecutorService

    override val chainId: Int
        get() = poW.chainId

    @Volatile
    private var stopped = false

    @Volatile
    private var currentMiningHeight: Long = 0

    @Volatile
    private var working = false
    private var task: Future<*>? = null

    override fun createCoinBase(height: Long): Transaction {
        return Transaction(
            data = PoWBios.UPDATE.encode().hex(),
            nonce = height
        )
    }

    override fun createHeader(parent: Block, createdAt: Long): Header {
        return HeaderImpl(
            hashPrev = parent.hash,
            coinbase = config.coinbase!!,
            createdAt = createdAt,
            height = parent.height + 1
        )
    }

    override fun finalizeBlock(parent: Block, block: Block): Block? {
        var nbits: Uint256
        var b = block
        poW.repo.reader.use { rd -> nbits = poW.bios.getNBits(rd, parent.hash) }
        val rd = Random()
        val nonce = ByteArray(NONCE_SIZE)
        log.info("start finish pow target = {}", nbits.data.hex())
        working = true
        while (PoW.compare(PoW.getPoWHash(b), nbits.data) > 0) {
            if (!working) {
                log.info("mining canceled")
                return null
            }
            rd.nextBytes(nonce)
            b = b.copy(header = b.header.impl.copy(nonce = nonce.hex()))
        }
        log.info("pow success")
        working = false
        return b
    }

    override fun start() {
        stopped = false
        minerExecutor = Executors.newSingleThreadScheduledExecutor()
        minerExecutor.scheduleAtFixedRate(Runnable {
            try {
                tryMine()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 0, Math.max(1, config.blockInterval / 4).toLong(), TimeUnit.SECONDS)
    }

    override fun stop() {
        if (stopped) return
        working = false
        minerExecutor!!.shutdown()
        try {
            minerExecutor!!.awaitTermination(MAX_SHUTDOWN_WAITING, TimeUnit.SECONDS)
            threadPool.awaitTermination(MAX_SHUTDOWN_WAITING, TimeUnit.SECONDS)
            log.info("miner stopped normally")
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        task = null
        stopped = true
    }

    fun tryMine() {
        if (!config.enableMining || stopped) {
            return
        }
        if (config.coinbase == null) {
            log.warn("pow miner: invalid coinbase address {}", config.coinbase)
            return
        }
        if (working || task != null) return
        poW.repo.reader.use { rd ->
            val best = rd.bestBlock
            log.debug("try to mining at height " + (best.height + 1))
            currentMiningHeight = best.height + 1
            val current = System.currentTimeMillis() / 1000
            if (current <= best.createdAt) return
        }
        val task = Runnable {
            try {
                poW.repo.reader.use { rd ->
                    val (block, infos) = createBlock(
                        rd,
                        rd.bestBlock
                    )
                    if (block != null) {
                        log.info("mining success block: {}", block.header)
                    }
                    eventBus.publish(NewBlockMined(block!!, infos))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                task = null
            }
        }
        this.task = threadPool.submit(task)
    }

    companion object {
        private val log = LoggerFactory.getLogger("pow-miner")
    }

    init {
        eventBus.subscribe(NewBestBlock::class.java) { (block) ->
            if (block.height >= currentMiningHeight) {
                // cancel mining
                working = false
                currentMiningHeight = 0
            }
        }
    }
}