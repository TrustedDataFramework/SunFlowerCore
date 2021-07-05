package org.tdf.sunflower.consensus.poa

import com.github.salpadding.rlpstream.Rlp
import org.tdf.common.util.HashUtil
import org.tdf.sunflower.types.Header

object PoaUtils {
    fun getRawHash(header: Header): ByteArray {
        val encoded = Rlp.encode(
            arrayOf<Any>(
                header.hashPrev, header.unclesHash, header.coinbase, header.stateRoot,
                header.transactionsRoot, header.receiptTrieRoot, header.logsBloom, header.difficulty,
                header.height, header.gasLimit, header.gasUsed, header.createdAt,
                header.mixHash, header.nonce
            )
        )
        return HashUtil.sha3(encoded)
    }
}