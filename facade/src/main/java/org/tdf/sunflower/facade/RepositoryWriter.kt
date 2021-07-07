package org.tdf.sunflower.facade

import org.tdf.sunflower.types.Block
import org.tdf.sunflower.types.TransactionIndex

interface RepositoryWriter : RepositoryReader {
    fun writeBlock(b: Block, infos: List<TransactionInfo>)
    fun saveGenesis(b: Block)
}