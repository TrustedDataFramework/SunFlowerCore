package org.tdf.sunflower.consensus.pos

import com.fasterxml.jackson.databind.JsonNode
import lombok.extern.slf4j.Slf4j
import org.tdf.sunflower.facade.AbstractConsensusEngine
import org.tdf.sunflower.types.ConsensusConfig
import org.tdf.common.util.HexBytes
import org.tdf.common.util.ascii
import org.tdf.common.util.sha3
import org.tdf.sunflower.state.Account
import org.tdf.sunflower.util.FileUtils
import org.tdf.sunflower.util.MapperUtil


@Slf4j(topic = "pos")
class PoS : AbstractConsensusEngine() {
    private lateinit var config: ConsensusConfig
    private lateinit var posValidator: PoSValidator
    private lateinit var posMiner: PoSMiner
    private lateinit var genesis: Genesis
    fun getMinerAddresses(stateRoot: HexBytes?): List<HexBytes> {
        return emptyList()
    }

    override val alloc: Map<HexBytes, Account>
        get() = genesis.alloc

    override val code: Map<HexBytes, HexBytes> = mutableMapOf()

    override fun init(config: ConsensusConfig) {
        this.config = config
        genesis = Genesis(config.genesisJson)
        genesisBlock = genesis.block
        posMiner = PoSMiner(accountTrie, eventBus, this.config, this)
        posMiner.repo = repo
        posMiner.transactionPool = transactionPool
        miner = posMiner
        posValidator = PoSValidator(accountTrie, posMiner, this.config.chainId)
        validator = posValidator

        val m = code as MutableMap
        val addr = "pos".ascii().sha3()

        val createCode = MapperUtil.OBJECT_MAPPER.readValue(FileUtils.getInputStream("pos-create-code.json"), JsonNode::class.java)
        val c = createCode["object"]


    }

    override val name: String
        get() = "pos"
}