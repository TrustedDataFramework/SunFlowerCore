package org.tdf.sunflower.sync

import com.github.salpadding.rlpstream.RlpCreator
import com.github.salpadding.rlpstream.RlpProps
import org.tdf.common.util.HexBytes

@RlpProps("stateRoot", "maxAccounts")
data class GetAccounts @RlpCreator constructor(val stateRoot: HexBytes, val maxAccounts: Int) {
}