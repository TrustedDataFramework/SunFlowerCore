package org.tdf.sunflower.consensus.poa


import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.tdf.common.crypto.ECKey
import org.tdf.common.util.ByteUtil
import org.tdf.common.util.FixedDelayScheduler
import org.tdf.common.util.RLPUtil
import org.tdf.sunflower.consensus.AbstractMiner
import org.tdf.sunflower.consensus.poa.config.PoAConfig
import org.tdf.sunflower.events.NewBlockMined
import org.tdf.sunflower.facade.RepositoryService
import org.tdf.sunflower.types.Block
import org.tdf.sunflower.types.Header
import org.tdf.sunflower.types.Transaction
import java.time.OffsetDateTime

class PoAMiner(private val poA: PoA) :
    AbstractMiner(poA.accountTrie, poA.eventBus, poA.config, poA.transactionPool) {

    private val config: PoAConfig
        get() = poA.config

    val repo: RepositoryService
        get() = poA.repo!!

    private val executor = FixedDelayScheduler("PoAMiner",
        Math.max(1, poA.config.blockInterval / 2).toLong()
    )

    override fun createHeader(parent: Block): Header {
        return Header
            .builder()
            .height(parent.height + 1)
            .createdAt(System.currentTimeMillis() / 1000)
            .coinbase(config.minerCoinBase)
            .hashPrev(parent.hash)
            .gasLimit(parent.gasLimit)
            .build()
    }

    override fun finalizeBlock(parent: Block, block: Block): Boolean {
        val rawHash = PoaUtils.getRawHash(block.header)
        val key = ECKey.fromPrivate(config.privateKey.bytes)
        val sig = key.sign(rawHash)
        block.extraData = RLPUtil.encode(
            arrayOf<Any>(
                sig.v,
                sig.r,
                sig.s
            )
        )
        if (config.threadId == PoA.GATEWAY_ID) {
            for (i in 1 until block.body.size) {
                val tx = block.body[i]
                poA.farmBaseTransactions.put(tx.hashHex, tx)
            }
        }
        return true
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

    fun tryMine() {
        if (!config.enableMining()) {
            return
        }
        repo.getWriter().use {
            val best = it.bestBlock
            val now = OffsetDateTime.now().toEpochSecond()

            // 判断是否轮到自己出块
            val p = poA.minerContract.getProposer(
                best.hash,
                now
            )
            if (p.address != config.minerCoinBase) return
            log.debug("try to mining at height " + (best.height + 1))
            val args: MutableMap<String, Long?> = HashMap()
            args["createdAt"] = now
            val b = createBlock(it.bestBlock, args)
            if (b.block != null) {
                log.info("mining success block: {}", b.block.header)
                it.writeBlock(b.block, b.infos)
                eventBus.publish(NewBlockMined(b.block, b.infos))
            }
        }
    }

    override fun createCoinBase(height: Long): Transaction {
        return Transaction.builder()
            .nonce(ByteUtil.longToBytesNoLeadZeroes(height))
            .value(poA.economicModel.getConsensusRewardAtHeight(height).noLeadZeroesData)
            .receiveAddress(config.minerCoinBase.bytes)
            .gasPrice(ByteUtil.EMPTY_BYTE_ARRAY)
            .gasLimit(ByteUtil.EMPTY_BYTE_ARRAY)
            .build()
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger("miner")
    }
}