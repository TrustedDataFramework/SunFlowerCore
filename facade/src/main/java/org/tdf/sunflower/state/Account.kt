package org.tdf.sunflower.state

import com.github.salpadding.rlpstream.annotation.RlpCreator
import com.github.salpadding.rlpstream.annotation.RlpProps
import org.tdf.common.types.Uint256
import org.tdf.common.util.HexBytes
import org.tdf.common.util.HashUtil

@RlpProps("nonce", "balance", "storageRoot", "contractHash")
data class Account @RlpCreator constructor(
    var nonce: Long = 0L,
    var balance: Uint256 = Uint256.ZERO,
    var storageRoot: HexBytes = HashUtil.EMPTY_TRIE_HASH_HEX,
    var contractHash: HexBytes = HashUtil.EMPTY_DATA_HASH_HEX
) {

    val isEmpty: Boolean
        get() = nonce == 0L && balance.isZero && contractHash == HashUtil.EMPTY_DATA_HASH_HEX && storageRoot == HashUtil.EMPTY_TRIE_HASH_HEX

    companion object {
        @JvmStatic
        fun emptyAccount(balance: Uint256): Account {
            return Account(0, balance, HashUtil.EMPTY_TRIE_HASH_HEX, HashUtil.EMPTY_DATA_HASH_HEX)
        }
    }
}