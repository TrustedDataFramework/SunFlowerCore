package org.tdf.sunflower.consensus

import org.tdf.common.types.Uint256
import org.tdf.sunflower.types.BlockValidateResult.Companion.fault
import org.tdf.common.util.ByteUtil
import org.tdf.sunflower.types.BlockValidateResult.Companion.success
import org.tdf.sunflower.state.StateTrie
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.state.Account
import org.tdf.sunflower.facade.RepositoryReader
import org.tdf.sunflower.facade.Validator
import org.tdf.sunflower.types.*
import org.tdf.sunflower.vm.CallContext
import org.tdf.sunflower.vm.CallData
import org.tdf.sunflower.vm.VMExecutor
import java.lang.Exception
import java.util.ArrayList

abstract class AbstractValidator(protected val accountTrie: StateTrie<HexBytes, Account>) : Validator {
    open val blockGasLimit: Long
        get() = 0

    protected fun commonValidate(rd: RepositoryReader, block: Block, parent: Block): BlockValidateResult {
        if (block.body.isEmpty()) return fault("missing block body")

        // a block should contains exactly one coin base transaction

        // validate coinbase
        if (block.coinbase != block.body[0].receiveAddress) {
            return fault("block coinbase not equals to coinbase tx receiver")
        }

        if(block.gasLimit != rd.genesis.gasLimit)
            return fault("invalid block gas limit")

        var isCoinbase = true
        for (t in block.body) {
            // validate transaction signature
            try {
                t.validate()
            } catch (e: Exception) {
                return fault(e.message!!)
            }
            if (!isCoinbase && !t.verifySig) {
                return fault("verify signature failed")
            }
            isCoinbase = false
        }
        if (!parent.isParentOf(block) || parent.height + 1 != block.height) {
            return fault("dependency is not parent of block")
        }
        if (parent.createdAt >= block.createdAt) {
            return fault(String.format("invalid timestamp %d at block height %d", block.createdAt, block.height))
        }
        if (Transaction.calcTxTrie(block.body) != block.transactionsRoot) {
            return fault("transactions root not match")
        }
        var totalFee = Uint256.ZERO
        val gas: Long = 0
        val results: MutableMap<HexBytes, VMResult> = mutableMapOf()
        var currentRoot: HexBytes
        var currentGas: Long = 0
        val receipts: MutableList<TransactionReceipt> = ArrayList()
        val bloom = Bloom()
        try {
            var tmp = accountTrie.createBackend(parent.header,  false, parent.stateRoot)
            val coinbase = block.body[0]
            for (tx in block.body.subList(1, block.body.size)) {
                if (currentGas > blockGasLimit) return fault("block gas overflow")
                var r: VMResult
                val executor = VMExecutor(
                    rd,
                    tmp,
                    CallContext.fromTx(tx),
                    CallData.fromTx(tx, false),
                    Math.min(blockGasLimit - currentGas, tx.gasLimit)
                )
                r = executor.execute()
                results[tx.hash] = r
                totalFee += r.fee
                currentGas += r.gasUsed
                val receipt = TransactionReceipt(
                    ByteUtil.longToBytesNoLeadZeroes(currentGas),
                    Bloom(), emptyList()
                )
                receipt.setGasUsed(r.gasUsed)
                receipt.executionResult = r.executionResult.bytes
                receipt.logInfoList = r.logs
                bloom.or(receipt.bloomFilter)
                currentRoot = tmp.merge()
                receipts.add(receipt)
                tmp = accountTrie.createBackend(parent.header,  root = currentRoot)
            }

            val executor = VMExecutor(rd, tmp, CallContext.fromTx(coinbase), CallData.fromTx(coinbase, true), 0)
            val r = executor.execute()
            currentGas += r.gasUsed
            val receipt = TransactionReceipt(
                ByteUtil.longToBytesNoLeadZeroes(currentGas),
                Bloom(), emptyList()
            )
            receipt.setGasUsed(r.gasUsed)
            receipt.executionResult = r.executionResult.bytes
            receipts.add(0, receipt)
            val rootHash = tmp.merge()
            if (rootHash != block.stateRoot) {
                return fault("state trie root not match")
            }

            // validate receipt trie root
            if (TransactionReceipt.calcReceiptsTrie(receipts) != block.receiptTrieRoot) {
                return fault("receipts trie root not match")
            }

            // validate bloom
            if (bloom != Bloom(block.logsBloom.bytes)) {
                return fault("logs bloom not match")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return fault("contract evaluation failed or " + e.message)
        }
        val infos: MutableList<TransactionInfo> = ArrayList()
        for (i in receipts.indices) {
            infos.add(TransactionInfo(receipts[i], block.hash.bytes, i))
        }
        return success(gas, totalFee, results, infos)
    }
}