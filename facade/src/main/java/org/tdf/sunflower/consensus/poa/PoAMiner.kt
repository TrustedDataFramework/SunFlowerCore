package org.tdf.sunflower.consensus.poa


import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.tdf.common.crypto.ECKey
import org.tdf.common.util.FixedDelayScheduler
import org.tdf.common.util.RLPUtil
import org.tdf.common.util.hex
import org.tdf.common.util.rlp
import org.tdf.sunflower.consensus.AbstractMiner
import org.tdf.sunflower.consensus.poa.config.PoAConfig
import org.tdf.sunflower.events.NewBlockMined
import org.tdf.sunflower.facade.RepositoryService
import org.tdf.sunflower.types.Block
import org.tdf.sunflower.types.Header
import org.tdf.sunflower.types.HeaderImpl
import org.tdf.sunflower.types.Transaction
import java.time.OffsetDateTime

class PoAMiner(private val poA: PoA) :
    AbstractMiner(poA.accountTrie, poA.eventBus, poA.config, poA.transactionPool) {

    override val chainId: Int
        get() = poA.chainId

    private val config: PoAConfig
        get() = poA.config

    val repo: RepositoryService
        get() = poA.repo

    private val executor = FixedDelayScheduler("PoAMiner",
        Math.max(1, poA.config.blockInterval / 2).toLong()
    )

    override fun createHeader(parent: Block, createdAt: Long): Header {
        return HeaderImpl(
            height = parent.height + 1,
            createdAt = createdAt,
            coinbase = config.minerCoinBase!!,
            hashPrev = parent.hash,
            gasLimit = parent.gasLimit,
        )
    }

    override fun finalizeBlock(parent: Block, block: Block): Block {
        val rawHash = PoaUtils.getRawHash(block.header)
        val key = ECKey.fromPrivate(config.privateKey!!.bytes)
        val sig = key.sign(rawHash)
        val extraData =
            arrayOf(
                sig.v,
                sig.r,
                sig.s
            ).rlp()

        if (config.threadId == PoA.GATEWAY_ID) {
            for (i in 1 until block.body.size) {
                val tx = block.body[i]
                poA.cache.put(tx.hash, tx)
            }
        }
        return Block(block.header.impl.copy(extraData = extraData.hex()), block.body)
    }

    @Synchronized
    override fun start() {
        executor.delay {
            try {
                this.tryMine()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Synchronized
    override fun stop() {
        executor.shutdownNow()
    }

    private fun tryMine() {
        if (!config.enableMining) {
            return
        }
        repo.writer.use {
            val best = it.bestBlock
            val now = OffsetDateTime.now().toEpochSecond()

            // 判断是否轮到自己出块
            val p = poA.minerContract.getProposer(
                it,
                best.hash,
                now
            )
            if (p.address != config.minerCoinBase) return
            log.debug("try to mining at height " + (best.height + 1))
            val b = createBlock(it, it.bestBlock, now)
            if (b.block != null) {
                log.info("mining success block: {}", b.block.header)
                it.writeBlock(b.block, b.infos)
                eventBus.publish(NewBlockMined(b.block, b.infos))
            }
        }
    }

    override fun createCoinBase(height: Long): Transaction {
        return Transaction(
            nonce = height,
            value = poA.model.rewardAt(height),
            receiveAddress = config.minerCoinBase!!,

        )
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger("miner")
    }
}