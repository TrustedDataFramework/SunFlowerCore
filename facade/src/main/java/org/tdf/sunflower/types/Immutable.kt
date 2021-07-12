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
import org.tdf.common.util.*
import org.tdf.sunflower.state.AddrUtil
import org.tdf.sunflower.types.TxUtils.*
import java.math.BigInteger
import java.util.function.Function

interface Header : Chained {
    /**
     * hash of parent block
     */
    override val hashPrev: H256

    /**
     * uncles list = rlp([])
     */
    val unclesHash: H256

    /**
     * miner
     */
    val coinbase: Address

    /**
     * root hash of state trie
     */
    val stateRoot: H256

    /**
     * root hash of transaction trie
     */
    val transactionsRoot: H256

    /**
     * receipts root
     */
    val receiptsRoot: H256

    /**
     * logs bloom
     */
    val logsBloom: H2048

    /**
     * difficulty value = EMPTY_BYTES
     */
    val difficulty: Long

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
    val nonce: HexBytes
    val encoded: ByteArray

    override val hash: H256

    companion object : Codec<Header> {
        override val encoder: Function<in Header, ByteArray>
            get() = Function { it.encoded }
        override val decoder: Function<ByteArray, out Header>
            get() = Function { Rlp.decode(it, HeaderImpl::class.java) }

    }

    val impl: HeaderImpl
}

@RlpProps(
    "hashPrev",
    "unclesHash",
    "coinbase",
    "stateRoot",
    "transactionsRoot",
    "receiptsRoot",
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
    override val hashPrev: H256 = ByteUtil.ZEROS_32,
    /**
     * uncles list = rlp([])
     */
    override val unclesHash: H256 = HashUtil.EMPTY_LIST_HASH.hex(),
    /**
     * miner
     */
    override val coinbase: Address = AddrUtil.empty(),
    /**
     * root hash of state trie
     */
    override val stateRoot: H256 = HashUtil.EMPTY_TRIE_HASH_HEX,
    /**
     * root hash of transaction trie
     */
    override val transactionsRoot: H256 = HashUtil.EMPTY_TRIE_HASH_HEX,

    /**
     * receipts root
     */
    override val receiptsRoot: H256 = HashUtil.EMPTY_TRIE_HASH_HEX,
    /**
     * logs bloom
     */
    override val logsBloom: H2048 = Bloom.EMPTY,

    /**
     * difficulty value = EMPTY_BYTES
     */
    override val difficulty: Long = 0,

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
    override val mixHash: HexBytes = ByteUtil.ZEROS_32,

    // = 8byte
    override val nonce: HexBytes = emptyNonce
) : Header {

    override val encoded: ByteArray by lazy {
        rlp()
    }

    override val hash: H256 by lazy {
        encoded.sha3().hex()
    }
    override val impl: HeaderImpl
        get() = this

    companion object {
        val emptyNonce = ByteArray(8).hex()
    }
}


@RlpProps("header", "body")
data class Block(val header: Header, val body: List<Transaction> = emptyList()) : Header by header {
    companion object {

        @RlpCreator
        @JvmStatic
        fun create(bin: ByteArray, streamId: Long): Block {
            val li = StreamId.asList(bin, streamId)
            return Block(
                li.valueAt(0, HeaderImpl::class.java),
                li.valueAt(1, Array<Transaction>::class.java).toList()
            )
        }

        val BEST_COMPARATOR: Comparator<Block> = Comparator { a, b ->
            if (a.height != b.height)
                return@Comparator a.height.compareTo(b.height)
            if (a.body.size != b.body.size) {
                return@Comparator a.body.size.compareTo(b.body.size)
            }
            return@Comparator a.hash.compareTo(b.hash)
        }

        val FAT_COMPARATOR: Comparator<Block> = Comparator { a, b ->
            if (a.height != b.height)
                return@Comparator a.height.compareTo(b.height)
            if (a.body.size != b.body.size) {
                return@Comparator -a.body.size.compareTo(b.body.size)
            }
            return@Comparator a.hash.compareTo(b.hash)
        }
    }
}

@RlpProps("status", "cumulativeGas", "logInfoList", "gasUsed", "result")
data class TransactionReceipt @RlpCreator constructor(
    val status: Int = 1,
    val cumulativeGas: Long = 0,
    val logInfoList: List<LogInfo> = emptyList(),
    val gasUsed: Long = 0,
    val result: HexBytes = HexBytes.empty(),
) {
    val bloom: Bloom by lazy {
        val b = Bloom()
        logInfoList.forEach {
            b.or(it.bloom)
        }
        b
    }

    val trie: ByteArray by lazy {
        arrayOf(status, cumulativeGas, bloom.data, logInfoList).rlp()
    }

    val encoded: ByteArray by lazy {
        rlp()
    }

    companion object {
        fun calcTrie(receipts: List<TransactionReceipt>): H256 {
            val receiptsTrie: Trie<ByteArray, ByteArray> = TrieImpl()
            if (receipts.isEmpty()) return HashUtil.EMPTY_TRIE_HASH_HEX
            for (i in receipts.indices) {
                receiptsTrie[i.rlp()] = receipts[i].trie
            }
            return receiptsTrie.commit()
        }

        fun bloomOf(receipts: List<TransactionReceipt>): Bloom {
            val r = Bloom()
            receipts.forEach {
                r.or(it.bloom)
            }
            return r
        }
    }
}


data class LogInfo(
    val address: Address = AddrUtil.empty(),
    val topics: List<H256>,
    val data: HexBytes = HexBytes.empty()
): RlpWritable {
    override fun writeToBuf(buf: RlpBuffer): Int {
        return buf.writeObject(arrayOf(address, topics, data))
    }

    val encoded: ByteArray by lazy {
        Rlp.encode(this)
    }

    val bloom: Bloom by lazy {
        val ret = Bloom.create(address.bytes.sha3())
        for (topic in topics) {
            ret.or(Bloom.create(topic.sha3().bytes))
        }
        ret
    }

    companion object {
        @RlpCreator
        @JvmStatic
        fun create(bin: ByteArray, streamId: Long): LogInfo{
            val li = StreamId.asList(bin, streamId)
            return LogInfo(
                li.hex(0),
                li.valueAt(1, Array<HexBytes>::class.java).map { it.h256() },
                li.hex(2)
            )
        }
    }
}

fun RlpList.hex(i: Int): HexBytes {
    return HexBytes.fromBytes(this.bytesAt(i))
}

fun RlpList.u256(i: Int): Uint256 {
    return Uint256.of(this.bytesAt(i))
}

typealias VRS = Triple<BigInteger, HexBytes, HexBytes>

val VRS.v get() = first
val VRS.r get() = second
val VRS.s get() = third

internal fun VRS.signature(): ECDSASignature {
    return ECDSASignature.fromComponents(r.bytes, s.bytes, getRealV(v))
}

internal fun VRS.chainId(): Int {
    return extractChainIdFromRawSignature(v, r.bytes, s.bytes)
}

data class Transaction(
    val nonce: Long = 0,
    val gasPrice: Uint256 = Uint256.ZERO,
    val gasLimit: Long = 0,
    val to: Address = AddrUtil.empty(),
    val value: Uint256 = Uint256.ZERO,
    val data: HexBytes = HexBytes.empty(),
    val vrs: VRS? = null
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
            if (transactions.isEmpty())
                return HashUtil.EMPTY_TRIE_HASH_HEX
            for (i in transactions.indices) {
                txsState[i.rlp()] = transactions[i].encoded
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
            require(li.size() == 9 || li.size() == 6) { "Invalid RLP elements count ${li.size()}" }


            var vrs: VRS? = null
            if (li.size() == 9) {
                val vData = li.bytesAt(6)
                val v = ByteUtil.bytesToBigInteger(vData)
                val r = li.bytesAt(7)
                val s = li.bytesAt(8)
                if (r.isEmpty() || s.isEmpty())
                    throw RuntimeException("invalid transaction, missing signature")
                vrs = VRS(v, r.hex(), s.hex())
            }


            return Transaction(
                li.longAt(0), li.u256(1),
                li.longAt(2).takeIf { it >= 0 } ?: throw RuntimeException("gas limit exceeds Long.MAX_VALUE"),
                li.hex(3), li.u256(4), li.hex(5),
                vrs
            )
        }

        override val encoder: Function<in Transaction, ByteArray>
            get() = Function { it.encoded }
        override val decoder: Function<ByteArray, out Transaction>
            get() = Function { Rlp.decode(it, Transaction::class.java) }
    }

    val verifySig: Boolean by lazy {
        val sig = signature ?: return@lazy false
        val key = ECKey.signatureToKey(rawHash, sig)
        // verify signature
        key.verify(rawHash, signature)
    }

    val signature: ECDSASignature? by lazy {
        vrs?.signature()
    }

    val chainId: Int? by lazy {
        vrs?.chainId()
    }

    fun validate() {
        require(to.size == 0 || to.size == ADDRESS_LENGTH) { "Receive address is not valid" }
        require(signature?.r?.bytes()?.size ?: 0 <= HASH_LENGTH) { "Signature R is not valid" }
        require(signature?.s?.bytes()?.size ?: 0 <= HASH_LENGTH) { "Signature S is not valid" }
        require(sender.size == ADDRESS_LENGTH) { "Sender is not valid" }
        require(gasLimit >= 0) { "gas limit cannot overflow" }
    }

    val sender: Address by lazy {
        signature?.let { ECKey.signatureToAddress(rawHash, it) }?.hex() ?: AddrUtil.empty()
    }

    val rawHash: ByteArray by lazy {
        encodedRaw.sha3()
    }

    val creation: Boolean = to.isEmpty()

    val contractAddress: Address?
        get() {
            if (!creation)
                return null
            return HashUtil.calcNewAddr(sender.bytes, nonce.bytes()).hex()
        }

    override val hash: H256 by lazy {
        encoded.sha3().hex()
    }

    val encodedRaw: ByteArray by lazy {
        // encoded raw of unsigned transaction
        val cid = chainId ?: return@lazy arrayOf(nonce, gasPrice, gasLimit, to, value, data).rlp()

        // encoded raw of signed transaction
        arrayOf(
            nonce, gasPrice, gasLimit,
            to, value, data,
            cid, 0, 0
        ).rlp()
    }

    val encoded: ByteArray by lazy {
        Rlp.encode(this)
    }

    override fun writeToBuf(buf: RlpBuffer): Int {
        val sig = signature ?: return buf.writeObject(
            arrayOf(
                nonce, gasPrice, gasLimit,
                to.bytes, value, data.bytes
            )
        )

        val v: Int = getV(chainId, sig)
        val r = sig.r
        val s = sig.s

        return buf.writeObject(
            arrayOf(
                nonce, gasPrice, gasLimit,
                to.bytes, value, data.bytes,
                v, r, s
            )
        )
    }
}

@RlpProps("receipt", "blockHash", "i")
data class TransactionIndex @RlpCreator constructor(
    val receipt: TransactionReceipt,
    val blockHash: HexBytes,
    val i: Int,
)