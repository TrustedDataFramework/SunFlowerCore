package org.tdf.sunflower.facade

import org.tdf.common.util.HexBytes
import org.tdf.sunflower.types.Block
import org.tdf.sunflower.types.Header
import org.tdf.sunflower.types.Transaction
import org.tdf.sunflower.types.TransactionInfo
import java.io.Closeable

interface RepositoryReader: Closeable{
    val genesis: Block
    fun containsHeader(hash: HexBytes): Boolean
    val bestHeader: Header
    val bestBlock: Block
    fun getHeaderByHash(hash: HexBytes): Header?
    fun getBlockByHash(hash: HexBytes): Block?
    fun getHeadersByHeight(height: Long): List<Header>
    fun getBlocksByHeight(height: Long): List<Block>
    fun getCanonicalBlock(height: Long): Block?
    fun getCanonicalHeader(height: Long): Header?

    fun getHeadersBetween(startHeight: Long, stopHeight: Long, limit: Int = Int.MAX_VALUE, descend: Boolean = false): List<Header>
    fun getBlocksBetween(startHeight: Long, stopHeight: Long, limit: Int = Int.MAX_VALUE, descend: Boolean = false): List<Block>

    // get transaction info with transaction included
    fun getTransactionInfo(hash: HexBytes): TransactionInfo?

    fun getTransaction(hash: HexBytes): Transaction? {
        return getTransactionInfo(hash)?.receipt?.transaction
    }

    // is transaction exists
    fun containsTransaction(hash: HexBytes): Boolean

    override fun close() {

    }
}