package org.tdf.sunflower.controller


import org.tdf.sunflower.types.Header
import org.tdf.sunflower.types.Transaction

data class TransactionResultDTO(
    val hash: String,
    val nonce: String,
    val blockHash: String,
    val blockNumber: String,
    val transactionIndex: String,
    val from: String,
    val to: String,
    val gas: String,
    val gasPrice: String,
    val value: String,
    val input: String,
) {

    companion object {
        fun create(h: Header, index: Int, tx: Transaction): TransactionResultDTO {
            return TransactionResultDTO(
                tx.hash.jsonHex,
                tx.nonce.jsonHexNum,
                h.hash.jsonHex,
                h.height.jsonHex,
                index.jsonHex,
                tx.sender.jsonHex,
                tx.receiveAddress.jsonHex,
                tx.gasLimit.jsonHexNum,
                tx.gasPrice.jsonHexNum,
                tx.value.jsonHexNum,
                tx.data.jsonHex
            )
        }
    }
}