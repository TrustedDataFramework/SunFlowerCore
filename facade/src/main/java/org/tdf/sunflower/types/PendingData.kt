package org.tdf.sunflower.types

import org.tdf.common.util.HexBytes

data class PendingData(
    val pending: List<Transaction>,
    val receipts: List<TransactionReceipt>,
    val current: HexBytes
)