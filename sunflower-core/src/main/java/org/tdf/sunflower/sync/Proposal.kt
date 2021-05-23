package org.tdf.sunflower.sync

import com.github.salpadding.rlpstream.RlpCreator
import com.github.salpadding.rlpstream.RlpProps
import org.tdf.sunflower.types.Block

@RlpProps("block")
data class Proposal @RlpCreator constructor(val block: Block) {
}