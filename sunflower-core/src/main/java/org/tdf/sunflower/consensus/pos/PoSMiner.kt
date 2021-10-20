package org.tdf.sunflower.consensus.pos

import org.slf4j.LoggerFactory
import org.tdf.common.event.EventBus
import org.tdf.sunflower.state.StateTrie
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.ApplicationConstants.MAX_SHUTDOWN_WAITING
import org.tdf.sunflower.state.Account
import org.tdf.sunflower.consensus.AbstractMiner
import org.tdf.sunflower.facade.RepositoryService
import kotlin.jvm.Volatile
import org.tdf.sunflower.facade.TransactionPool
import org.tdf.sunflower.consensus.Proposer
import org.tdf.sunflower.events.NewBlockMined
import org.tdf.sunflower.types.*
import java.lang.Exception
import java.time.OffsetDateTime
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class PoSMiner(
    accountTrie: StateTrie<HexBytes, Account>,
    eventBus: EventBus,
    private val config: ConsensusConfig,
    private val pos: PoS
) : AbstractMiner(accountTrie, eventBus, config, pos.transactionPool) {
    var minerAddress: HexBytes? = null
    lateinit var repo: RepositoryService

    @Volatile
    private var stopped = false
    private lateinit var minerExecutor: ScheduledExecutorService
    lateinit var transactionPool: TransactionPool

    override fun createHeader(parent: Block, createdAt: Long): Header {
        return HeaderImpl(
            height = parent.height + 1,
            createdAt = createdAt,
            coinbase = config.coinbase ?: throw RuntimeException("poa miner: private key not set"),
            hashPrev = parent.hash,
            gasLimit = parent.gasLimit,
        )
    }

    override fun finalizeBlock(parent: Block, block: Block): Block {
        return block
    }

    override fun start() {
        stopped = false
        minerExecutor = Executors.newSingleThreadScheduledExecutor()
        minerExecutor.scheduleAtFixedRate({
            try {
                tryMine()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 0, config.blockInterval.toLong(), TimeUnit.SECONDS)
    }

    override fun stop() {
        if (stopped) return
        minerExecutor.shutdown()
        try {
            minerExecutor.awaitTermination(MAX_SHUTDOWN_WAITING, TimeUnit.SECONDS)
            log.info("miner stopped normally")
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        stopped = true
    }

    private fun tryMine() {
        if (!config.enableMining || stopped) {
            return
        }
        if (config.coinbase == null) {
            log.warn("pos miner: invalid coinbase address {}", config.coinbase)
            return
        }
        try {
            repo.reader.use { rd ->
                val best = rd.bestBlock
                // 判断是否轮到自己出块
                val o = getProposer(
                    best,
                    OffsetDateTime.now().toEpochSecond()
                )?.takeIf { it.address == minerAddress } ?: return
                log.debug("try to mining at height " + (best.height + 1))
                val (block, indices) = createBlock(rd, rd.bestBlock, System.currentTimeMillis() / 1000)
                if (block != null) {
                    log.info("mining success block: {}", block.header)
                }
                eventBus.publish(NewBlockMined(block!!, indices))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getProposer(parent: Block, currentEpochSeconds: Long): Proposer? {
        return null
    }

    override fun createCoinBase(height: Long): Transaction {
        return Transaction()
    }

    override val chainId: Int = config.chainId

    companion object {
        private val log = LoggerFactory.getLogger("miner")
    }
}