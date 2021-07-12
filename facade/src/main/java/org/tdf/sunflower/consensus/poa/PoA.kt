package org.tdf.sunflower.consensus.poa

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

// poa is a minimal non-trivial consensus engine
class PoA : AbstractConsensusEngine() {
    val model = PoAModel()

    lateinit var config: PoAConfig
    lateinit var genesis: Genesis


    lateinit var minerContract: Authentication
    override val builtins: MutableList<BuiltinContract> = mutableListOf()

    override val alloc: Map<HexBytes, Account>
        get() = genesis.alloc


    override fun init(config: ConsensusConfig) {
        this.config = from(config)
        genesis = Genesis(config.genesisJson)
        genesisBlock = genesis.block

        minerContract = Authentication(
            genesis.miners,
            Constants.POA_AUTHENTICATION_ADDR,
            accountTrie,
            repo,
            this.config
        )
        builtins.add(minerContract)
        miner = PoAMiner(this)
        validator = PoAValidator(accountTrie, this)
    }

    override val name: String
        get() = "poa"
}