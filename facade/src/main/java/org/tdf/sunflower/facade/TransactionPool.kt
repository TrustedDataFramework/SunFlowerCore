package org.tdf.sunflower.facade

import com.google.common.cache.Cache
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.types.Header
import org.tdf.sunflower.types.PendingData
import org.tdf.sunflower.types.Transaction
import org.tdf.sunflower.vm.Backend

typealias Dropped = Pair<Transaction, String>

val Dropped.err: String get() = second
val Dropped.tx: Transaction get() = first

interface TransactionPool {
    // collect transactions into transaction pool, return errors
    fun collect(rd: RepositoryReader, transactions: Collection<Transaction>): Map<HexBytes, String>
    fun collect(rd: RepositoryReader, tx: Transaction): Map<HexBytes, String> {
        return collect(rd, setOf(tx))
    }

    // pop at most n packable transactions
    // if limit < 0, pop all transactions
    fun pop(rd: RepositoryReader, parentHeader: Header, timestamp: Long): PendingData

    // recollect pending data
    fun reset(parent: Header)

    fun current(): Backend?

    val dropped: Cache<HexBytes, Dropped>
}