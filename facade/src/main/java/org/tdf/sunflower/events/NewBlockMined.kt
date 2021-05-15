package org.tdf.sunflower.events

import org.tdf.sunflower.types.Block
import org.tdf.sunflower.types.TransactionInfo

// proposal new block and failed transactions to peers
data class NewBlockMined(val block: Block, val infos: List<TransactionInfo>)