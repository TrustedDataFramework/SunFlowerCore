package org.tdf.sunflower.consensus

import org.tdf.common.event.EventBus
import org.tdf.common.types.Uint256
import org.tdf.common.util.ByteUtil
import org.tdf.sunflower.state.StateTrie
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.state.Account
import org.tdf.sunflower.facade.TransactionPool
import org.tdf.sunflower.facade.Miner
import org.tdf.sunflower.facade.RepositoryReader
import org.tdf.sunflower.types.*
import org.tdf.sunflower.vm.CallContext
import org.tdf.sunflower.vm.CallData
import org.tdf.sunflower.vm.VMExecutor

abstract class AbstractMiner(
    protected val accountTrie: StateTrie<HexBytes, Account>,
    protected val eventBus: EventBus,
    private val config: ConsensusConfig,
    private val pool: TransactionPool
) : Miner {

    protected abstract fun createCoinBase(height: Long): Transaction
    protected abstract fun createHeader(parent: Block, createdAt: Long): Header
    protected abstract fun finalizeBlock(parent: Block, block: Block): Block?

    // TODO:  2. 增加打包超时时间
    protected fun createBlock(rd: RepositoryReader, parent: Block, createdAt: Long = System.currentTimeMillis() / 1000): BlockCreateResult {
        val (transactionList, rs, current, bloom) = pool.pop(parent.header)
        val receipts = rs.toMutableList()

        if (!config.allowEmptyBlock && transactionList.isEmpty()) {
            pool.reset(parent.header)
            return BlockCreateResult.empty()
        }
        var header = createHeader(parent, createdAt)

        // get a trie at parent block's state
        // modifications to the trie will not persisted until flush() called
        val coinbase = createCoinBase(parent.height + 1)
        val tmp = current ?: accountTrie.createBackend(parent.header,false, parent.stateRoot)

        val totalFee = receipts
            .map { it.transaction.gasPrice * it.gasUsedAsU256 }
            .reduceOrNull { x, y -> x + y }
            ?: Uint256.ZERO

        // add fee to miners account
        val c = coinbase.copy(value = coinbase.value + totalFee)
        val body = transactionList.toMutableList()
        body.add(0, c)
        val ctx = CallContext.fromTx(c)
        val callData = CallData.fromTx(c, true)


        val res: VMResult
        val executor = VMExecutor(rd, tmp, ctx, callData, 0)
        res = executor.execute()
        val lastGas = if (receipts.isEmpty()) 0 else receipts[receipts.size - 1].cumulativeGasLong
        val receipt = TransactionReceipt( // coinbase consume none gas
            ByteUtil.longToBytesNoLeadZeroes(res.gasUsed + lastGas),
            Bloom(), emptyList()
        )
        receipt.executionResult = res.executionResult.bytes
        receipt.transaction = coinbase
        receipts.add(0, receipt)

        // calculate state root and receipts root
        val stateRoot = tmp.merge()
        val receiptTrieRoot = TransactionReceipt.calcReceiptsTrie(receipts)
        val txRoot = Transaction.calcTxTrie(body)
        // persist modifications of trie to database

        header = header.impl.copy(
            stateRoot = stateRoot, receiptTrieRoot = receiptTrieRoot, transactionsRoot = txRoot,
            logsBloom = HexBytes.fromBytes(bloom.data)
        )

        val blk = finalizeBlock(parent, Block(header, body)) ?: return BlockCreateResult.empty()
        // the mined block cannot be modified any more

        val infos = receipts.mapIndexed { i, r -> TransactionInfo(r, blk.hash.bytes, i) }
        return BlockCreateResult(blk, infos)
    }
}