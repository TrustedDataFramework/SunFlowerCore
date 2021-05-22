package org.tdf.sunflower.sync

import org.tdf.common.util.HexBytes
import org.tdf.rlpstream.RlpCreator
import org.tdf.rlpstream.RlpProps

@RlpProps("stateRoot", "maxAccounts")
data class GetAccounts @RlpCreator constructor(val stateRoot: HexBytes, val maxAccounts: Int) {
}