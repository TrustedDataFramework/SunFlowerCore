package org.tdf.sunflower.types

import com.github.salpadding.rlpstream.*
import com.github.salpadding.rlpstream.annotation.RlpCreator
import com.github.salpadding.rlpstream.annotation.RlpProps
import org.tdf.common.crypto.ECDSASignature
import org.tdf.common.crypto.ECKey
import org.tdf.common.serialize.Codec
import org.tdf.common.trie.Trie
import org.tdf.common.trie.TrieImpl
import org.tdf.common.types.Chained
import org.tdf.common.types.Hashed
import org.tdf.common.types.Uint256
import org.tdf.common.util.BigIntegers
import org.tdf.common.util.ByteUtil
import org.tdf.common.util.HashUtil
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.state.Address
import org.tdf.sunflower.types.TxUtils.*
import java.math.BigInteger
import java.util.function.Function


interface Header: Chained {
    /**
     * hash of parent block
     */
    override val hashPrev: HexBytes

    /**
     * uncles list = rlp([])
     */
    val unclesHash: HexBytes

    /**
     * miner
     */
    val coinbase: HexBytes

    /**
     * root hash of state trie
     */
    val stateRoot: HexBytes

    /**
     * root hash of transaction trie
     */
    val transactionsRoot: HexBytes

    /**
     * receipts root
     */
    val receiptTrieRoot: HexBytes

    /**
     * logs bloom
     */
    val logsBloom: HexBytes

    /**
     * difficulty value = EMPTY_BYTES
     */
    val difficulty: HexBytes

    /**
     * block height
     */
    val height: Long
    val gasLimit: Long
    val gasUsed: Long

    /**
     * unix epoch when the block mined
     */
    val createdAt: Long
    val extraData: HexBytes
    val mixHash: HexBytes
    val nonce: Long
    val encoded: ByteArray
    override val hash: HexBytes

    companion object: Codec<Header> {
        override val encoder: Function<in Header, ByteArray>
            get() = Function { it.encoded }
        override val decoder: Function<ByteArray, out Header>
            get() =  Function { Rlp.decode(it, HeaderImpl::class.java) }

    }

    val impl: HeaderImpl
}

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
data class HeaderImpl @RlpCreator constructor(
    /**
     * hash of parent block
     */
    override val hashPrev: HexBytes = ByteUtil.ZEROS_32,
    /**
     * uncles list = rlp([])
     */
    override val unclesHash: HexBytes = HexBytes.fromBytes(HashUtil.EMPTY_LIST_HASH),
    /**
     * miner
     */
    override val coinbase: HexBytes = Address.empty(),
    /**
     * root hash of state trie
     */
    override val stateRoot: HexBytes = HashUtil.EMPTY_TRIE_HASH_HEX,
    /**
     * root hash of transaction trie
     */
    override val transactionsRoot: HexBytes = HashUtil.EMPTY_TRIE_HASH_HEX,

    /**
     * receipts root
     */
    override val receiptTrieRoot: HexBytes = HashUtil.EMPTY_TRIE_HASH_HEX,
    /**
     * logs bloom
     */
    override val logsBloom: HexBytes = Bloom.EMPTY,

    /**
     * difficulty value = EMPTY_BYTES
     */
    override val difficulty: HexBytes = HexBytes.empty(),

    /**
     * block height
     */
    override val height: Long = 0,
    override val gasLimit: Long = 0,
    override val gasUsed: Long = 0,


    /**
     * unix epoch when the block mined
     */
    override val createdAt: Long = 0,

    // <= 32 bytes
    override val extraData: HexBytes = HexBytes.empty(),

    // = 32byte
    override val mixHash: HexBytes = HexBytes.empty(),

    // = 8byte
    override val nonce: Long = 0
) : Header {

    override val encoded: ByteArray by lazy {
        Rlp.encode(this)
    }

    override val hash: HexBytes by lazy{
        HashUtil.sha3Hex(encoded)
    }
    override val impl: HeaderImpl
        get() = this
}

data class Block(val header: Header, val body: List<Transaction> = emptyList()): Header by header {
    companion object {
        val BEST_COMPARATOR : Comparator<Block> = Comparator { a, b ->
            if (a.height != b.height)
                return@Comparator a.height.compareTo(b.height)
            if (a.body.size != b.body.size) {
                return@Comparator a.body.size.compareTo(b.body.size)
            }
            return@Comparator a.hash.compareTo(b.hash)
        }

        val FAT_COMPARATOR : Comparator<Block> = Comparator { a, b ->
            if (a.height != b.height)
                return@Comparator a.height.compareTo(b.height)
            if (a.body.size != b.body.size) {
                return@Comparator -a.body.size.compareTo(b.body.size)
            }
            return@Comparator a.hash.compareTo(b.hash)
        }
    }


}

fun RlpList.hex(i: Int): HexBytes {
    return HexBytes.fromBytes(this.bytesAt(i))
}

fun RlpList.u256(i: Int): Uint256 {
    return Uint256.of(this.bytesAt(i))
}

data class Transaction(
    val nonce: Long = 0,
    val gasPrice: Uint256 = Uint256.ZERO,
    val gasLimit: Long = 0,
    val receiveAddress: HexBytes = Address.empty(),
    val value: Uint256 = Uint256.ZERO,
    val data: HexBytes = HexBytes.empty(),
    val chainId: Int? = null,
    val signature: ECDSASignature? = null,
) : RlpWritable, Hashed {
    companion object : Codec<Transaction> {
        const val HASH_LENGTH = 32
        const val ADDRESS_LENGTH = 20

        /**
         * Since EIP-155, we could encode chainId in V
         */
        const val CHAIN_ID_INC = 35
        const val LOWER_REAL_V = 27

        @JvmStatic
        fun calcTxTrie(transactions: List<Transaction>): HexBytes {
            val txsState: Trie<ByteArray, ByteArray> = TrieImpl()
            if (transactions.isEmpty()) return HexBytes.fromBytes(HashUtil.EMPTY_TRIE_HASH)
            for (i in transactions.indices) {
                txsState[Rlp.encodeInt(i)] = transactions[i].encoded
            }
            return txsState.commit()
        }

        @RlpCreator
        @JvmStatic
        fun create(bin: ByteArray, streamId: Long): Transaction {
            return create(StreamId.asList(bin, streamId))
        }

        fun create(rlp: ByteArray): Transaction {
            return Rlp.decode(rlp, Transaction::class.java)
        }

        fun create(li: RlpList): Transaction {
            require(li.size() <= 9) { "Too many RLP elements" }

            var chainId: Int? = null
            var sig: ECDSASignature? = null
            if (li.size() >= 7 && li.bytesAt(6).isNotEmpty()) {
                val vData = li.bytesAt(6)
                val v = ByteUtil.bytesToBigInteger(vData)
                val r = li.bytesAt(7)
                val s = li.bytesAt(8)
                chainId = extractChainIdFromRawSignature(v, r, s)

                if (r.isNotEmpty() && s.isNotEmpty())
                    throw RuntimeException("invalid transaction, missing signature")
                sig = ECDSASignature.fromComponents(r, s, getRealV(v))
            }


            return Transaction(
                li.longAt(0), li.u256(1),
                li.longAt(2).takeIf { it >= 0 } ?: throw RuntimeException("gas limit exceeds Long.MAX_VALUE"),
                li.hex(3), li.u256(4), li.hex(5),
                chainId, sig
            )
        }

        override val encoder: Function<in Transaction, ByteArray>
            get() = Function { it.encoded }
        override val decoder: Function<ByteArray, out Transaction>
            get() = Function { Rlp.decode(it, Transaction::class.java) }
    }

    private fun RlpBuffer.writeObjects(vararg objects: Any): Int {
        return this.writeObject(arrayOf(objects))
    }

    private fun BigInteger.unsigned(): ByteArray {
        return BigIntegers.asUnsignedByteArray(this)
    }

    val verifySig: Boolean by lazy {
        val key = ECKey.signatureToKey(rawHash, signature)
        // verify signature
        key.verify(rawHash, signature)
    }

    fun validate() {
        require(receiveAddress.size() == ADDRESS_LENGTH) { "Receive address is not valid" }
        require(signature?.r?.unsigned()?.size ?: 0 <= HASH_LENGTH) { "Signature R is not valid" }
        require(signature?.s?.unsigned()?.size ?: 0 <= HASH_LENGTH) { "Signature S is not valid" }
        require(sender.size() == ADDRESS_LENGTH) { "Sender is not valid" }
    }

    val sender: HexBytes by lazy {
        HexBytes.fromBytes(ECKey.signatureToAddress(rawHash, signature))
    }

    val rawHash: ByteArray by lazy {
        HashUtil.sha3(encodedRaw)
    }

    val creation: Boolean = receiveAddress.isEmpty

    val contractAddress: HexBytes?
        get() {
            if (!creation)
                return null
            return HashUtil.calcNewAddrHex(sender.bytes, nonce)
        }

    override val hash: HexBytes by lazy {
        HashUtil.sha3Hex(encoded)
    }

    val encodedRaw: ByteArray by lazy {
        Rlp.encode(
            arrayOf(
                nonce, gasPrice, gasLimit,
                receiveAddress, value, data,
                chainId, 0, 0
            )
        )
    }

    val encoded: ByteArray by lazy {
        Rlp.encode(this)
    }

    override fun writeToBuf(buf: RlpBuffer): Int {
        val v: Int = getV(chainId, signature)
        val r = signature?.r ?: BigInteger.ZERO
        val s = signature?.s ?: BigInteger.ZERO

        return buf.writeObjects(
            nonce, gasPrice, gasLimit,
            receiveAddress.bytes, value, data.bytes,
            v, r, s
        )
    }
}