package org.tdf.sunflower.types

import com.github.salpadding.rlpstream.*
import com.github.salpadding.rlpstream.annotation.RlpCreator
import com.github.salpadding.rlpstream.annotation.RlpProps
import org.tdf.common.crypto.ECDSASignature
import org.tdf.common.crypto.ECKey
import org.tdf.common.types.Chained
import org.tdf.common.types.Uint256
import org.tdf.common.util.ByteUtil
import org.tdf.common.util.HashUtil
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.state.Address
import org.tdf.sunflower.types.Transaction.extractChainIdFromRawSignature
import org.tdf.sunflower.types.Transaction.getRealV
import java.math.BigInteger


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

fun RlpList.hex(i: Int): HexBytes {
    return HexBytes.fromBytes(this.bytesAt(i))
}

fun RlpList.u256(i: Int): Uint256 {
    return Uint256.of(this.bytesAt(i))
}

data class TransactionFields(
    val nonce: Long,
    val gasPrice: Uint256,
    val gasLimit: Long,
    val receiveAddress: HexBytes,
    val value: Uint256,
    val data: HexBytes,
    val chainId: Int,
    val signature: ECDSASignature,
    val hash: HexBytes
) : RlpWritable {
    companion object {
        @RlpCreator
        @JvmStatic
        fun create(bin: ByteArray, streamId: Long): TransactionFields {
            return create(StreamId.asList(bin, streamId))
        }

        fun create(li: RlpList): TransactionFields {
            if (li.size() < 7 || li.bytesAt(6).isEmpty())
                throw RuntimeException("invalid transaction, missing signature")

            val vData = li.bytesAt(6)
            val v = ByteUtil.bytesToBigInteger(vData)
            val r = li.bytesAt(7)
            val s = li.bytesAt(8)
            val chainId = extractChainIdFromRawSignature(v, r, s)
            if (r.isEmpty() || s.isEmpty())
                throw RuntimeException("invalid transaction, missing signature")
            val sig = ECDSASignature.fromComponents(r, s, getRealV(v))

            return TransactionFields(
                li.longAt(0), li.u256(1),
                li.longAt(2).takeIf { it >= 0 } ?: throw RuntimeException("gas limit exceeds Long.MAX_VALUE"),
                li.hex(3), li.u256(4), li.hex(5),
                chainId, sig, HashUtil.sha3Hex(li.encoded)
            )
        }
    }

    private fun RlpBuffer.writeObjects(vararg objects: Any): Int {
        return this.writeObject(arrayOf(objects))
    }

    val verifySig: Boolean by lazy{
        val key = ECKey.signatureToKey(rawHash, signature)
        // verify signature
        key.verify(rawHash, signature)
    }

    fun validate() {
        require(receiveAddress.size() == Transaction.ADDRESS_LENGTH) { "Receive address is not valid" }
    }

    val rawHash: ByteArray by lazy {
        HashUtil.sha3(encodedRaw)
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

    override fun writeToBuf(buf: RlpBuffer): Int {
        val v: Int

        var encodeV: Int = signature.v - Transaction.LOWER_REAL_V
        encodeV += chainId * 2 + Transaction.CHAIN_ID_INC
        v = encodeV
        val r: BigInteger = signature.r
        val s: BigInteger = signature.s

        return buf.writeObjects(
            nonce, gasPrice, gasLimit,
            receiveAddress.bytes, value, data.bytes,
            v, r, s
        )
    }
}