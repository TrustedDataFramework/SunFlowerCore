package org.tdf.sunflower.controller

import org.tdf.sunflower.types.Block
import org.tdf.sunflower.types.TransactionInfo

class TransactionReceiptDTOExt(block: Block?, txInfo: TransactionInfo) : TransactionReceiptDTO(block, txInfo) {
    var returnData: String
    var error: String

    init {
        returnData = txInfo.receipt.executionResult.jsonHex
        error = txInfo.receipt.error
    }
}