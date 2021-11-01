package org.tdf.sunflower.consensus.pow

import com.fasterxml.jackson.databind.JsonNode
import org.tdf.common.util.HexBytes
import org.tdf.common.util.hex
import org.tdf.sunflower.types.AbstractGenesis
import org.tdf.sunflower.types.Block
import org.tdf.sunflower.types.HeaderImpl

class Genesis(parsed: JsonNode) : AbstractGenesis(parsed) {
    companion object {
        val MAX_N_BITS: HexBytes = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff".hex()
    }

    val nbits: HexBytes
        get() = parsed["nbits"]?.asText()?.hex() ?: MAX_N_BITS

    override val block: Block
        get() {
            if (nbits.size != 32) throw RuntimeException("invalid nbits size should be 32")
            val h = HeaderImpl(
                hashPrev = parentHash,
                createdAt = timestamp
            )
            return Block(h)
        }
}