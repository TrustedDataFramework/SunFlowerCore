package org.tdf.sunflower.types

import com.github.salpadding.rlpstream.Rlp
import com.github.salpadding.rlpstream.annotation.RlpCreator
import com.github.salpadding.rlpstream.annotation.RlpProps
import org.tdf.common.types.Chained
import org.tdf.common.util.ByteUtil
import org.tdf.common.util.HashUtil
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.state.Address


@RlpProps(
    "hashPrev",
    "unclesHash",
    "coinbase",
    "stateRoot",
    "transactionsRoot",
    "receiptTrieRoot",
    "logsBloom",
    "difficulty",
    "height",
    "gasLimit",
    "gasUsed",
    "createdAt",
    "extraData",
    "mixHash",
    "nonce"
)
data class ImmutableHeader @RlpCreator constructor(
    /**
     * hash of parent block
     */
    private var hashPrev: HexBytes = ByteUtil.ZEROS_32,
    /**
     * uncles list = rlp([])
     */
    val unclesHash: HexBytes = HexBytes.fromBytes(HashUtil.EMPTY_LIST_HASH),
    /**
     * miner
     */
    val coinbase: HexBytes = Address.empty(),
    /**
     * root hash of state trie
     */
    val stateRoot: HexBytes = HashUtil.EMPTY_TRIE_HASH_HEX,
    /**
     * root hash of transaction trie
     */
    val transactionsRoot: HexBytes = HashUtil.EMPTY_TRIE_HASH_HEX,

    /**
     * receipts root
     */
    val receiptTrieRoot: HexBytes = HashUtil.EMPTY_TRIE_HASH_HEX,
    /**
     * logs bloom
     */
    val logsBloom: HexBytes = Bloom.EMPTY,

    /**
     * difficulty value = EMPTY_BYTES
     */
    val difficulty: HexBytes = HexBytes.empty(),

    /**
     * block height
     */
    val height: Long = 0,
    val gasLimit: HexBytes = HexBytes.EMPTY,
    val gasUsed: Long = 0,


    /**
     * unix epoch when the block mined
     */
    val createdAt: Long = 0,

    // <= 32 bytes
    val extraData: HexBytes = HexBytes.empty(),

    // = 32byte
    val mixHash: HexBytes = HexBytes.empty(),

    // = 8byte
    val nonce: HexBytes = HexBytes.empty()
) : Chained {
    private var hash: HexBytes? = null

    override fun getHashPrev(): HexBytes {
        return hashPrev
    }

    override fun getHash(): HexBytes {
        if (hash == null) {
            val h = HexBytes.fromBytes(
                HashUtil.sha3(Rlp.encode(this))
            )
            hash = h
            return h
        }
        return hash!!
    }
}

@RlpProps("header", "body")
class ImmutableBlock @RlpCreator constructor(
    val header: Header,
    body: Array<Transaction>
) {
    val body: List<Transaction> = body.toList()
}