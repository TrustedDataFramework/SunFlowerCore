package org.tdf.sunflower.consensus.pos

import org.slf4j.LoggerFactory
import org.tdf.common.event.EventBus
import org.tdf.common.types.Constants
import org.tdf.common.types.Uint256
import org.tdf.sunflower.state.StateTrie
import org.tdf.common.util.HexBytes
import org.tdf.common.util.hex
import org.tdf.common.util.u256
import org.tdf.sunflower.ApplicationConstants.MAX_SHUTDOWN_WAITING
import org.tdf.sunflower.state.Account
import org.tdf.sunflower.consensus.AbstractMiner
import org.tdf.sunflower.facade.RepositoryService
import kotlin.jvm.Volatile
import org.tdf.sunflower.facade.TransactionPool
import org.tdf.sunflower.consensus.Proposer
import org.tdf.sunflower.consensus.pow.PoW
import org.tdf.sunflower.consensus.pow.PoWMiner
import org.tdf.sunflower.events.NewBlockMined
import org.tdf.sunflower.facade.RepositoryReader
import org.tdf.sunflower.types.*
import org.tdf.sunflower.vm.VMExecutor
import org.tdf.sunflower.vm.abi.Abi
import java.lang.Exception
import java.math.BigInteger
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class PoSMiner(
    accountTrie: StateTrie<HexBytes, Account>,
    eventBus: EventBus,
    private val config: ConsensusConfig,
    private val pos: PoS
) : AbstractMiner(accountTrie, eventBus, config, pos.transactionPool) {
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

    override fun finalizeBlock(rd: RepositoryReader, parent: Block, block: Block): Block {
        var nbits = pos.getDifficulty(rd, parent)
        var b = block
        val rd = Random()
        val nonce = ByteArray(Constants.NONCE_SIZE)
        log.info("start finish pow target = {}", nbits.byte32.hex())
        while (PoW.compare(PoW.getPoWHash(b), nbits.byte32) > 0) {
            rd.nextBytes(nonce)
            b = b.copy(header = b.header.impl.copy(nonce = nonce.hex()))
        }
        log.info("pow success")
        return b
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
        }, 0, 1, TimeUnit.SECONDS)
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
            repo.writer.use {
                log.debug("writer required")
                val best = it.bestBlock
                log.debug("best block = {}", best)
                val now = OffsetDateTime.now().toEpochSecond()
                val p = pos.getProposer(it, best, now)
                log.debug("pos.getProposer() = {}", p)

                // 判断是否轮到自己出块
                val o = p?.takeIf { x -> x.first == config.coinbase } ?: return

                val (block, indices) = createBlock(it, it.bestBlock, now, o.third)
                if (block != null) {
                    log.info("mining success block: {}", block.header)
                    it.writeBlock(block, indices)
                    eventBus.publish(NewBlockMined(block, indices))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun createCoinBase(height: Long): Transaction {
        val cb = config.coinbase ?: throw RuntimeException("pos miner: coinbase not found")
        val f = pos.consensusAbi.first { it.name == "__coinbase__" && it is Abi.Function }

        return Transaction(
            value = Uint256.ZERO,
            to = pos.consensusAddr,
            data = (f.encodeSignature() + Abi.Entry.Param.encodeList(f.inputs, cb.bytes)).hex(),
            gasLimit = VMExecutor.GAS_UNLIMITED
        )
    }

    override val chainId: Int = config.chainId

    companion object {
        private val log = LoggerFactory.getLogger("miner")
    }
}