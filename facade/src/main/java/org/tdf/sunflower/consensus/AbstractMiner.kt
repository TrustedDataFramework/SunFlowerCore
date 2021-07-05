package org.tdf.sunflower.consensus

import org.slf4j.LoggerFactory
import org.tdf.common.event.EventBus
import org.tdf.common.types.Uint256
import org.tdf.sunflower.types.BlockCreateResult.Companion.empty
import org.tdf.common.util.ByteUtil
import org.tdf.sunflower.vm.CallContext.Companion.fromTx
import org.tdf.sunflower.vm.CallData.Companion.fromTx
import org.tdf.sunflower.state.StateTrie
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.state.Account
import org.tdf.sunflower.facade.TransactionPool
import org.tdf.sunflower.facade.Miner
import org.tdf.sunflower.facade.RepositoryReader
import org.tdf.sunflower.types.*
import org.tdf.sunflower.vm.VMExecutor
import java.util.ArrayList

abstract class AbstractMiner(
    protected val accountTrie: StateTrie<HexBytes, Account>,
    protected val eventBus: EventBus,
    private val config: ConsensusConfig,
    protected val pool: TransactionPool
) : Miner {

    protected abstract fun createCoinBase(height: Long): Transaction
    protected abstract fun createHeader(parent: Block): Header
    protected abstract fun finalizeBlock(parent: Block, block: Block): Boolean

    // TODO:  2. 增加打包超时时间
    protected fun createBlock(rd: RepositoryReader, parent: Block, headerArgs: Map<String, *>): BlockCreateResult {
        val (transactionList, rs, current, bloom) = pool.pop(parent.header)
        val receipts = rs.toMutableList()

        if (!config.allowEmptyBlock && transactionList.isEmpty()) {
            pool.reset(parent.header)
            return empty()
        }
        val header = createHeader(parent)
        for ((field, value) in headerArgs) {
            val setter = header.javaClass
                .getMethod(
                    "set" + field.substring(0, 1).uppercase() + field.substring(1),
                    value!!.javaClass
                )
            setter.invoke(header, value)
        }
        val b = Block(header)

        // get a trie at parent block's state
        // modifications to the trie will not persisted until flush() called
        val coinbase = createCoinBase(parent.height + 1)
        val tmp = current ?: accountTrie.createBackend(parent.header, null, false, parent.stateRoot)
        b.body = transactionList
        b.body.add(0, coinbase)
        val totalFee = receipts.stream()
            .map { x: TransactionReceipt -> x.transaction.gasPriceAsU256.times(x.gasUsedAsU256) }
            .reduce(Uint256.ZERO) { obj: Uint256, word: Uint256 -> obj.plus(word) }

        // add fee to miners account
        coinbase.setValue(coinbase.valueAsUint.plus(totalFee))
        val ctx = fromTx(coinbase)
        val callData = fromTx(coinbase, true)
        tmp.headerCreatedAt = System.currentTimeMillis() / 1000
        header.setCreatedAt(tmp.headerCreatedAt)
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
        b.stateRoot = tmp.merge()
        b.receiptTrieRoot = TransactionReceipt.calcReceiptsTrie(receipts)
        // persist modifications of trie to database
        b.resetTransactionsRoot()
        b.logsBloom = HexBytes.fromBytes(bloom.data)

        // the mined block cannot be modified any more
        if (!finalizeBlock(parent, b)) {
            return empty()
        }
        val infos: MutableList<TransactionInfo> = ArrayList()
        for (i in receipts.indices) {
            infos.add(TransactionInfo(receipts[i], b.hash.bytes, i))
        }
        return BlockCreateResult(b, infos)
    }

    companion object {
        private val log = LoggerFactory.getLogger("miner")
    }
}