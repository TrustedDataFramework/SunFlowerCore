package org.tdf.sunflower.types

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.JsonNode
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.facade.PropertiesWrapper
import org.tdf.sunflower.facade.PropertyLike
import org.tdf.sunflower.util.FileUtils
import org.tdf.sunflower.util.MapperUtil
import java.util.*

open class ConsensusConfig(val properties: PropertyLike) {
    @JvmField
    protected val reader: PropertyReader = PropertyReader(properties)

    constructor(properties: Properties) : this(PropertiesWrapper(properties))

    val name: String
        get() = reader.getAsLowerCased("name")

    val blockInterval: Int
        get() = reader.getAsInt("block-interval")

    val enableMining: Boolean
        get() {
            return reader.getAsBool("enable-mining")
        }

    val privateKey: HexBytes?
        get() = reader.getAsPrivate("private-key")

    val allowEmptyBlock: Boolean
        get() {
            return reader.getAsBool("allow-empty-block")
        }

    open val coinbase: HexBytes?
        get() = reader.getAsAddress("miner-coin-base")

    val blocksPerEra: Int
        get() = reader.getAsInt("blocks-per-era")
    val maxMiners: Int
        get() = reader.getAsInt("max-miners")


    val genesisJson: JsonNode by lazy {
        val objectMapper = MapperUtil.OBJECT_MAPPER
            .enable(JsonParser.Feature.ALLOW_COMMENTS)
        val stream = FileUtils.getInputStream(properties.getProperty("genesis")!!)
        objectMapper.readValue(stream, JsonNode::class.java)
    }

    val blockGasLimit: Long
        get() = genesisJson["gasLimit"]?.asLong() ?: DEFAULT_BLOCK_GAS_LIMIT

    companion object {
        const val DEFAULT_BLOCK_GAS_LIMIT: Long = 60000000
    }

}