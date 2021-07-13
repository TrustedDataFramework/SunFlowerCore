package org.tdf.sunflower

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.core.env.Environment
import org.tdf.common.crypto.ECKey
import org.tdf.common.types.Uint256
import org.tdf.sunflower.facade.PropertyLike
import org.tdf.sunflower.types.ConsensusConfig
import org.tdf.sunflower.types.PropertyReader
import org.tdf.sunflower.util.FileUtils
import org.tdf.sunflower.util.MapperUtil

class AppConfig(private val properties: PropertyLike) {
    constructor(env: Environment) : this(EnvWrapper(env))

    private val reader = PropertyReader(properties)

    val myKey = ECKey()

    val eip8: Boolean
        get() = true

    val peerConnectionTimeout: Int
        get() = 2000
    val peerChannelReadTimeout: Long
        get() = 30L
    val nodeId: ByteArray
        get() = myKey.nodeId
    val maxActivePeers: Int
        get() = 16

    fun peerCapabilities(): List<String> {
        return listOf("eth")
    }

    val defaultP2PVersion: Int
        get() = 5
    val listenPort: Int
        get() = 8545
    val rlpxMaxFrameSize: Int
        get() = 32768
    val p2pPingInterval: Int
        get() = 5

    data class EnvWrapper(val env: Environment) : PropertyLike {
        override fun getProperty(key: String): String? {
            return env.getProperty(key)
        }
    }

    val p2pTransactionCacheSize: Int = reader.getAsInt("sunflower.cache.p2p.transaction")
    val p2pProposalCacheSize: Int = reader.getAsInt("sunflower.cache.p2p.proposal")

    val blockGasLimit: Long by lazy {
        val objectMapper = MapperUtil.OBJECT_MAPPER
            .enable(JsonParser.Feature.ALLOW_COMMENTS)
        val `in` = FileUtils.getInputStream(
            properties.getProperty("sunflower.consensus.genesis")
        )
        val n = objectMapper.readValue(`in`, JsonNode::class.java)
        if (n["gasLimit"] == null) ConsensusConfig.DEFAULT_BLOCK_GAS_LIMIT else n["gasLimit"].asLong()
    }


    val vmGasPrice: Uint256 = reader.getAsU256("sunflower.vm.gas-price", Uint256.ZERO)
    val isVmDebug: Boolean = reader.getAsBool("sunflower.vm.debug")
    val isTrieSecure: Boolean = reader.getAsBool("sunflower.trie.secure")

    companion object {
        fun get(): AppConfig {
            return INSTANCE
        }

        lateinit var INSTANCE: AppConfig
    }
}