package org.tdf.sunflower.consensus.poa

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.TickerMode
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.tdf.common.crypto.ECKey
import org.tdf.common.util.BigIntegers
import org.tdf.common.util.ByteUtil
import org.tdf.common.util.RLPUtil
import org.tdf.sunflower.consensus.AbstractMiner
import org.tdf.sunflower.consensus.poa.config.PoAConfig
import org.tdf.sunflower.events.NewBlockMined
import org.tdf.sunflower.facade.RepositoryService
import org.tdf.sunflower.types.Block
import org.tdf.sunflower.types.Header
import org.tdf.sunflower.types.Transaction
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.TimeUnit

class PoAMiner(private val poA: PoA) : AbstractMiner(poA.accountTrie, poA.eventBus, poA.config, poA.transactionPool) {

    private val config: PoAConfig
        get() = poA.config

    val repo: RepositoryService
        get() = poA.repo!!

    private var ch: ReceiveChannel<Unit>? = null

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
                poA.farmBaseTransactions.add(block.body[i])
            }
        }
        return true
    }

    @Synchronized
    override fun start() {
        if (ch != null) {
            return
        }

        ch = ticker(
            TimeUnit.SECONDS.toMillis(config.blockInterval.toLong()),
            mode = TickerMode.FIXED_DELAY
        )

        GlobalScope.launch {
            for (c in ch!!) {
                try {
                    tryMine()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    @Synchronized
    override fun stop() {
        if (ch == null) return
        ch!!.cancel()
        ch = null
    }

    fun tryMine() {
        if (!config.enableMining()) {
            return
        }
        repo.getReader().use {
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