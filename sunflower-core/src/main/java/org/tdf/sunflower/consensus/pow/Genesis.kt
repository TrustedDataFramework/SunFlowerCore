package org.tdf.sunflower.consensus.pow

import com.fasterxml.jackson.databind.JsonNode
import org.tdf.sunflower.types.AbstractGenesis
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.types.Block
import org.tdf.sunflower.types.HeaderImpl

internal fun String.hex(): HexBytes {
    return HexBytes.fromHex(this)
}

class Genesis(parsed: JsonNode) : AbstractGenesis(parsed) {
    companion object {
        val MAX_N_BITS: HexBytes = HexBytes.fromHex("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")
    }

    val nbits: HexBytes
        get() = parsed["nbits"]?.asText()?.hex() ?: MAX_N_BITS

    override val block: Block
        get() {
            if (nbits.size() != 32) throw RuntimeException("invalid nbits size should be 32")
            val h = HeaderImpl(
                hashPrev = parentHash,
                createdAt = timestamp
            )
            return Block(h)
        }
}