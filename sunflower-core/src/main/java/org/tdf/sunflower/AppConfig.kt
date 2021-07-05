package org.tdf.sunflower

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.JsonNode
import org.tdf.sunflower.facade.PropertyLike
import org.tdf.sunflower.types.PropertyReader
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.core.env.Environment
import org.tdf.common.crypto.ECKey
import org.tdf.common.types.Uint256
import org.tdf.sunflower.types.ConsensusConfig
import org.tdf.sunflower.util.FileUtils

class AppConfig(private val properties: PropertyLike) {
    constructor(env: Environment): this(EnvWrapper(env))

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

    val trieCacheSize: Int
        get() = reader.getAsInt("sunflower.cache.trie", 32)
    val p2pTransactionCacheSize: Int
        get() = reader.getAsInt("sunflower.cache.p2p.transaction", 128)
    val p2pProposalCacheSize: Int
        get() = reader.getAsInt("sunflower.cache.p2p.proposal", 128)

    val blockGasLimit: Long by lazy {
            val objectMapper = ObjectMapper()
                .enable(JsonParser.Feature.ALLOW_COMMENTS)
            val `in` = FileUtils.getInputStream(
                properties.getProperty("sunflower.consensus.genesis")
            )
            val n = objectMapper.readValue(`in`, JsonNode::class.java)
           if (n["gasLimit"] == null) ConsensusConfig.DEFAULT_BLOCK_GAS_LIMIT else n["gasLimit"].asLong()
        }

    val maxFrames: Long
        get() = reader.getAsLong("sunflower.vm.max-frames")
    val maxContractCallDepth: Int
        get() = reader.getAsInt("sunflower.vm.max-contract-call-depth")
    val vmGasPrice: Uint256
        get() = reader.getAsU256("sunflower.vm.gas-price", Uint256.ZERO)
    val isVmDebug: Boolean
        get() = reader.getAsBool("sunflower.vm.debug")
    val isTrieSecure: Boolean
        get() = reader.getAsBool("sunflower.trie.secure")

    companion object {
        @JvmStatic
        fun get(): AppConfig {
            return INSTANCE
        }

        lateinit var INSTANCE: AppConfig
    }
}