package org.tdf.sunflower.consensus

import org.slf4j.LoggerFactory
import org.tdf.common.event.EventBus
import org.tdf.common.types.Uint256
import org.tdf.common.util.HexBytes
import org.tdf.common.util.hex
import org.tdf.common.util.u256
import org.tdf.sunflower.facade.Miner
import org.tdf.sunflower.facade.RepositoryReader
import org.tdf.sunflower.facade.TransactionInfo
import org.tdf.sunflower.facade.TransactionPool
import org.tdf.sunflower.state.Account
import org.tdf.sunflower.state.AddrUtil
import org.tdf.sunflower.state.StateTrie
import org.tdf.sunflower.types.*
import org.tdf.sunflower.vm.CallContext
import org.tdf.sunflower.vm.CallData
import org.tdf.sunflower.vm.VMExecutor
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

abstract class AbstractMiner(
    protected val accountTrie: StateTrie<HexBytes, Account>,
    protected val eventBus: EventBus,
    private val config: ConsensusConfig,
    private val pool: TransactionPool
) : Miner {

    protected abstract fun createCoinBase(height: Long): Transaction
    protected abstract fun createHeader(parent: Block, createdAt: Long): Header

    open fun finalizeBlock(rd: RepositoryReader, parent: Block, block: Block): Block {
        return block
    }

    protected abstract val chainId: Int

    // TODO:  2. 增加打包超时时间
    protected fun createBlock(
        rd: RepositoryReader,
        parent: Block,
        createdAt: Long = System.currentTimeMillis() / 1000,
        deadline: Long = Long.MAX_VALUE
    ): BlockCreateResult {
        val (txs, rs, current) = pool.pop(rd, parent.header, createdAt)
        val zipped = txs.zip(rs)
        val receipts = rs.toMutableList()

        if (!config.allowEmptyBlock && txs.isEmpty()) {
            return BlockCreateResult.empty()
        }

        var header = createHeader(parent, createdAt)

        // get a trie at parent block's state
        // modifications to the trie will not persisted until flush() called
        val coinbase = createCoinBase(parent.height + 1)
        val tmp = current ?: accountTrie.createBackend(parent = parent.header)

        val totalFee = zipped
            .map { it.first.gasPrice * it.second.gasUsed.u256() }
            .reduceOrNull { x, y -> x + y }
            ?: Uint256.ZERO

        // add fee to miners account
        val c = coinbase.copy(value = totalFee)
        val body = txs.toMutableList()
        body.add(0, c)
        val ctx = CallContext.fromTx(c, chainId, createdAt, header.coinbase, rd.createBlockHashMap(parent.hash).toMap())
        val callData = CallData.fromTx(c)

        tmp.addBalance(AddrUtil.empty(), totalFee)
        val res: VMResult
        val executor = VMExecutor.create(rd, tmp, ctx, callData, c.gasLimit)
        res = executor.execute()
        val lastGas = receipts.getOrNull(receipts.size - 1)?.cumulativeGas ?: 0L

        val receipt = TransactionReceipt( // coinbase consume none gas
            cumulativeGas = res.gasUsed + lastGas,
            logInfoList = res.logs,
            gasUsed = res.gasUsed,
            result = res.executionResult
        )

        receipts.add(0, receipt)

        // calculate state root and receipts root
        val stateRoot = tmp.merge()
        val receiptTrieRoot = TransactionReceipt.calcTrie(receipts)
        val txRoot = Transaction.calcTxTrie(body)
        // persist modifications of trie to database

        header = header.impl.copy(
            stateRoot = stateRoot, receiptsRoot = receiptTrieRoot, transactionsRoot = txRoot,
            logsBloom = TransactionReceipt.bloomOf(receipts).data.hex()
        )

        val duration =
            if (deadline == Long.MAX_VALUE) Long.MAX_VALUE else (deadline * 1000 - System.currentTimeMillis())

        val blkFuture = CompletableFuture.supplyAsync {
            finalizeBlock(rd, parent, Block(header, body))
        }

        val blk = try {
            blkFuture.get(duration, TimeUnit.SECONDS) ?: return BlockCreateResult.empty()
        } catch (e: Exception) {
            e.printStackTrace()
            blkFuture.cancel(true)
            return BlockCreateResult.empty()
        }

        txs.forEach {
            log.debug("pack transaction {} into block at {}, sender = {}, nonce = {}", it.hash, blk.height, it.sender)
        }

        // the mined block cannot be modified any more
        val infos = receipts.mapIndexed { i, r -> TransactionInfo(TransactionIndex(r, blk.hash, i), blk.body[i]) }

        // reset log index
        var i = 0
        receipts.forEach {
            it.logInfoList.forEach { t ->
                t.logIndex = i
                i++
            }
        }
        return BlockCreateResult(blk, infos)
    }

    companion object {
        val log = LoggerFactory.getLogger("miner")
    }
}