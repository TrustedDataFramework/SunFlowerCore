package org.tdf.sunflower.consensus.pos

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import org.tdf.sunflower.types.AbstractGenesis
import org.tdf.common.util.HexBytes
import org.tdf.common.util.hex
import org.tdf.sunflower.types.Block
import org.tdf.sunflower.types.HeaderImpl

class Genesis(parsed: JsonNode) : AbstractGenesis(parsed) {
    override val block: Block
        @JsonIgnore
        get() {
            val h = HeaderImpl(gasLimit = gasLimit, createdAt = timestamp)
            return Block(h)
        }

    data class MinerInfo(
        var address: HexBytes,
        var vote: Long = 0,

        ) {
        companion object {
            fun fromJson(n: JsonNode): MinerInfo {
                return MinerInfo(
                    n["address"].asText().hex(),
                    n["vote"].asLong()
                )
            }
        }
    }

    val miners: List<MinerInfo>
        get() {
            return getArray("miners")
                .map { MinerInfo.fromJson(it) }
        }

}