package org.tdf.sunflower.sync

import org.tdf.rlpstream.RlpCreator
import org.tdf.rlpstream.RlpProps
import org.tdf.sunflower.types.Block

@RlpProps("block")
data class Proposal @RlpCreator constructor(val block: Block){
}