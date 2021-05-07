package org.tdf.sunflower.facade

import org.tdf.common.util.HexBytes
import org.tdf.sunflower.types.Header
import org.tdf.sunflower.types.PendingData
import org.tdf.sunflower.types.Transaction

interface TransactionPool {
    // collect transactions into transaction pool, return errors
    fun collect(transactions: Collection<Transaction>): Map<HexBytes, String>
    fun collect(tx: Transaction): Map<HexBytes, String> {
        return collect(setOf(tx))
    }

    // pop at most n packable transactions
    // if limit < 0, pop all transactions
    fun pop(parentHeader: Header): PendingData

    // recollect pending data
    fun reset(parent: Header)
}