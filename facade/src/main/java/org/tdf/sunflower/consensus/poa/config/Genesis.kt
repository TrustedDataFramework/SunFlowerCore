package org.tdf.sunflower.consensus.poa.config

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import org.tdf.sunflower.types.AbstractGenesis
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.types.Block
import org.tdf.sunflower.types.Header

class Genesis(parsed: JsonNode) : AbstractGenesis(parsed) {
    private fun getAddressList(field: String): List<HexBytes> {
        return getArray(field)
            .map { HexBytes.fromHex(it["addr"].asText()) }
    }

    val miners: List<HexBytes>
        get() = getAddressList("miners")
    val validators: List<HexBytes>
        get() = getAddressList("validators")

    override val block: Block
        @JsonIgnore
        get() {
            val h = Header.builder()
                .gasLimit(
                    gasLimitHex
                )
                .createdAt(timestamp)
                .build()
            return Block(h)
        }
}