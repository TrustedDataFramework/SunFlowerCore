package org.tdf.sunflower.consensus.poa

import org.tdf.common.util.rlp
import org.tdf.common.util.sha3
import org.tdf.sunflower.types.Header

object PoaUtils {
    fun getRawHash(header: Header): ByteArray {
        return arrayOf(
                header.hashPrev, header.unclesHash, header.coinbase, header.stateRoot,
                header.transactionsRoot, header.receiptsRoot, header.logsBloom, header.difficulty,
                header.height, header.gasLimit, header.gasUsed, header.createdAt,
                header.mixHash, header.nonce
            ).rlp().sha3()
    }
}