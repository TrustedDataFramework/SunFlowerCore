package org.tdf.sunflower.sync

import org.tdf.common.util.HexBytes
import org.tdf.rlpstream.RlpCreator
import org.tdf.rlpstream.RlpProps

@RlpProps("bestBlockHeight", "bestBlockHash", "genesisBlockHash", "prunedHeight", "prunedHash")
data class Status @RlpCreator constructor(
    val bestBlockHeight: Long,
    val bestBlockHash: HexBytes,
    val genesisBlockHash: HexBytes,
    val prunedHeight: Long,
    val prunedHash: HexBytes,
) {
}