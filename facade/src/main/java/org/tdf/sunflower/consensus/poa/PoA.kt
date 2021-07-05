package org.tdf.sunflower.consensus.poa

import com.fasterxml.jackson.databind.JsonNode
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.tdf.common.util.FixedDelayScheduler
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.consensus.poa.config.Genesis
import org.tdf.sunflower.consensus.poa.config.PoAConfig
import org.tdf.sunflower.consensus.poa.config.PoAConfig.Companion.from
import org.tdf.sunflower.facade.AbstractConsensusEngine
import org.tdf.sunflower.state.Account
import org.tdf.sunflower.state.Authentication
import org.tdf.sunflower.state.BuiltinContract
import org.tdf.sunflower.state.Constants
import org.tdf.sunflower.types.ConsensusConfig
import org.tdf.sunflower.types.Transaction
import org.tdf.sunflower.util.MapperUtil
import java.net.HttpURLConnection
import java.net.URL

// poa is a minimal non-trivial consensus engine
class PoA : AbstractConsensusEngine() {
    private val mapper = MapperUtil.OBJECT_MAPPER
    val model = PoAModel()

    lateinit var config: PoAConfig
    lateinit var genesis: Genesis


    lateinit var minerContract: Authentication
    lateinit var validatorContract: Authentication

    override val builtins: MutableList<BuiltinContract> = mutableListOf()

    private val executor = FixedDelayScheduler("gateway", 30)

    override val alloc: Map<HexBytes, Account>
        get() = genesis.alloc


    @JvmField
    val cache: Cache<HexBytes, Transaction> = CacheBuilder
        .newBuilder()
        .maximumSize(32)
        .build()

    override fun init(config: ConsensusConfig) {
        this.config = from(config)
        genesis = Genesis(config.genesisJson)
        genesisBlock = genesis.block
        if (this.config.threadId != 0 && this.config.threadId != GATEWAY_ID) {
            executor.delay {
                try {
                    repo.reader.use { rd ->
                        val url = URL(this.config.gatewayNode)
                        val connection = url.openConnection() as HttpURLConnection
                        connection.setRequestProperty("accept", "application/json")
                        val responseStream = connection.inputStream
                        var n = mapper.readValue(responseStream, JsonNode::class.java)
                        n = n["data"]
                        for (i in 0 until n.size()) {
                            val tx = Transaction(
                                HexBytes.decode(n[i].textValue())
                            )
                            if (!rd.containsTransaction(tx.hashHex)) {
                                transactionPool.collect(rd, tx)
                            }
                        }
                    }
                } catch (e: Exception) {
                }
            }

        }
        minerContract = Authentication(
            genesis.miners,
            Constants.POA_AUTHENTICATION_ADDR,
            accountTrie,
            repo,
            this.config
        )
        validatorContract = Authentication(
            genesis.validators,
            Constants.VALIDATOR_CONTRACT_ADDR,
            accountTrie,
            repo,
            this.config
        )
        builtins.add(validatorContract)
        builtins.add(minerContract)
        miner = PoAMiner(this)
        validator = PoAValidator(accountTrie, this)
    }

    override val chainId: Int
        get() = if (config.threadId == 0) super.chainId else config.threadId
    override val name: String
        get() = "poa"

    companion object {
        const val GATEWAY_ID = 1339
    }
}