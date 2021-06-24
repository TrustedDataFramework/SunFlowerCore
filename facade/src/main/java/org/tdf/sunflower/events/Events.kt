package org.tdf.sunflower.events

import org.tdf.sunflower.types.Block
import org.tdf.sunflower.types.Transaction
import org.tdf.sunflower.types.TransactionInfo


// proposal new block and failed transactions to peers
data class NewBlockMined(val block: Block, val infos: List<TransactionInfo>)

data class NewBestBlock(val block: Block)

data class NewBlocksReceived(val blocks: List<Block>)

// when receive new transaction in pools, broadcast to peers
data class NewTransactionsCollected(val transactions: List<Transaction>)