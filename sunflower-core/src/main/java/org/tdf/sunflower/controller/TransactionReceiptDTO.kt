package org.tdf.sunflower.controller

import com.fasterxml.jackson.annotation.JsonInclude
import org.tdf.sunflower.controller.JsonRpc.LogFilterElement
import org.tdf.sunflower.types.Block
import org.tdf.sunflower.types.Transaction
import org.tdf.sunflower.types.TransactionInfo

data class TransactionReceiptDTO(
    val transactionHash // hash of the transaction.
    : String? = null,
    val transactionIndex // integer of the transactions index position in the block.
    : String? = null,
    val blockHash // hash of the block where this transaction was in.
    : String? = null,
    val blockNumber // block number where this transaction was in.
    : String? = null,
    val from // 20 Bytes - address of the sender.
    : String? = null,
    val to // 20 Bytes - address of the receiver. null when its a contract creation transaction.
    : String? = null,
    val cumulativeGasUsed // The total amount of gas used when this transaction was executed in the block.
    : String? = null,
    val gasUsed // The amount of gas used by this specific transaction alone.
    : String? = null,
    val contractAddress // The contract address created, if the transaction was a contract creation, otherwise  null .
    : String? = null,
    val logs // Array of log objects, which this transaction generated.
    : List<LogFilterElement>? = null,
    val logsBloom // 256 Bytes - Bloom filter for light clients to quickly retrieve related logs.
    : String? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val status //  either 1 (success) or 0 (failure) (post Byzantium)
    : String? = null,
) {


    companion object {
        fun failed(tx: Transaction, reason: String): TransactionReceiptDTO {
            return TransactionReceiptDTO(
                transactionHash = tx.hash.jsonHex,
                from = tx.sender.jsonHex,
                to = tx.receiveAddress.takeIf { !it.isEmpty }?.jsonHex,
                contractAddress = tx.contractAddress?.jsonHex,
                status = "0x0"
            )
        }

        fun create(block: Block?, txInfo: TransactionInfo): TransactionReceiptDTO {
            val receipt = txInfo.receipt
            val tx = receipt.transaction
            val logs: Array<LogFilterElement?> = arrayOfNulls(receipt.logInfoList.size)

            for (i in logs.indices) {
                val logInfo = receipt.logInfoList[i]
                logs[i] = LogFilterElement.create(
                    logInfo, block, txInfo.index,
                    txInfo.receipt.transaction, i
                )
            }

            return TransactionReceiptDTO(
                tx.hash.jsonHex, txInfo.index.jsonHex, txInfo.blockHash?.jsonHex,
                block?.height?.jsonHex, tx.sender.jsonHex, tx.receiveAddress.takeIf { !it.isEmpty }?.jsonHex,
                receipt.cumulativeGas.jsonHexNum, receipt.gasUsed.jsonHexNum, tx.contractAddress?.jsonHex,
                logs.toList().requireNoNulls(), receipt.bloomFilter.data.jsonHex, (1).jsonHex
            )
        }
    }
}