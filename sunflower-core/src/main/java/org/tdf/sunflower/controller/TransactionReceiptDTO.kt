package org.tdf.sunflower.controller

import com.fasterxml.jackson.annotation.JsonInclude
import org.tdf.sunflower.controller.JsonRpc.LogFilterElement
import org.tdf.sunflower.types.Block
import org.tdf.sunflower.types.TransactionInfo

open class TransactionReceiptDTO(block: Block?, txInfo: TransactionInfo) {
    val transactionHash // hash of the transaction.
            : String?
    val transactionIndex // integer of the transactions index position in the block.
            : String
    var blockHash // hash of the block where this transaction was in.
            : String? = null
    var blockNumber // block number where this transaction was in.
            : String? = null
    val from // 20 Bytes - address of the sender.
            : String?
    val to // 20 Bytes - address of the receiver. null when its a contract creation transaction.
            : String?
    val cumulativeGasUsed // The total amount of gas used when this transaction was executed in the block.
            : String
    val gasUsed // The amount of gas used by this specific transaction alone.
            : String
    val contractAddress // The contract address created, if the transaction was a contract creation, otherwise  null .
            : String?
    val logs // Array of log objects, which this transaction generated.
            : Array<LogFilterElement?>
    val logsBloom // 256 Bytes - Bloom filter for light clients to quickly retrieve related logs.
            : String?

    @JsonInclude(JsonInclude.Include.NON_NULL)
    val status //  either 1 (success) or 0 (failure) (post Byzantium)
            : String


    init {
        val receipt = txInfo.receipt
        transactionHash = receipt.transaction.hash.jsonHex
        transactionIndex = txInfo.index.jsonHex
        cumulativeGasUsed = receipt.cumulativeGas.jsonHexNum
        gasUsed = receipt.gasUsed.jsonHexNum
        contractAddress = receipt.transaction.contractAddress.jsonHex

        from = receipt.transaction.sender.jsonHex
        to = receipt.transaction.receiveAddress.jsonHex
        logs = arrayOfNulls(receipt.logInfoList.size)

        if (block != null) {
            blockNumber = block.height.jsonHex
            blockHash = txInfo.blockHash.jsonHex
        } else {
            blockNumber = null
            blockHash = null
        }

        for (i in logs.indices) {
            val logInfo = receipt.logInfoList[i]
            logs[i] = LogFilterElement(
                logInfo, block, txInfo.index,
                txInfo.receipt.transaction, i
            )
        }
        logsBloom = receipt.bloomFilter.data.jsonHex
        status = "0x1"

//        if (receipt.hasTxStatus()) { // post Byzantium
//            root = null;
//            status = receipt.isTxStatusOK() ? "0x1" : "0x0";
//            root = toJsonHex(receipt.getPostTxState());
//        } else { // pre Byzantium
//            status = null;
//        }
    }
}