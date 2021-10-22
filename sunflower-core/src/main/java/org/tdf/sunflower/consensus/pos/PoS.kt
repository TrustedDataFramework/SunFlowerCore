package org.tdf.sunflower.consensus.pos

import com.fasterxml.jackson.databind.JsonNode
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import lombok.extern.slf4j.Slf4j
import org.tdf.common.util.*
import org.tdf.sunflower.facade.AbstractConsensusEngine
import org.tdf.sunflower.facade.RepositoryReader
import org.tdf.sunflower.types.ConsensusConfig
import org.tdf.sunflower.state.Account
import org.tdf.sunflower.state.AddrUtil
import org.tdf.sunflower.types.Header
import org.tdf.sunflower.util.FileUtils
import org.tdf.sunflower.util.MapperUtil
import org.tdf.sunflower.vm.abi.Abi
import java.math.BigInteger


@Slf4j(topic = "pos")
class PoS : AbstractConsensusEngine() {
    private lateinit var config: ConsensusConfig
    private lateinit var posValidator: PoSValidator
    private lateinit var posMiner: PoSMiner
    private lateinit var genesis: Genesis

    lateinit var consensusAbi: Abi
    val consensusAddr  = "pos".ascii().sha3().tail20().hex()
    val cache = CacheBuilder.newBuilder().maximumSize(128).build<HexBytes, List<Pair<HexBytes, BigInteger>>>()


    fun getProposer(rd: RepositoryReader, parent: Header, now: Long, interval: Long): Triple<HexBytes, Long, Long>?{
       val candidates = getCandidates(rd, parent)
        val w = accountTrie.createWrapper(rd, parent, consensusAbi, consensusAddr)
        val end = Math.min(candidates.size, POS_MAX_CANDIDATES)
        val r = w.call("getProposer", parent.coinbase.bytes, parent.createdAt.toBigInteger(), candidates.map { it.first.bytes }.subList(0, end), now.toBigInteger(), interval.toBigInteger())
        if((r[0] as ByteArray).hex() == AddrUtil.empty())
            return null
        var e = r[2] as BigInteger

        if(e > Long.MAX_VALUE.toBigInteger())
            e = Long.MAX_VALUE.toBigInteger()
        return Triple((r[0] as ByteArray).hex(), (r[1] as BigInteger).longValueExact(), e.longValueExact())
    }

    fun getCandidates(rd: RepositoryReader, parent: Header): List<Pair<HexBytes, BigInteger>> {
        val origin = parent.height / POS_INTERVAL * POS_INTERVAL
        val h = rd.getAncestor(parent.hash, origin)

        return cache.get(h.hash) {
            val w = accountTrie.createWrapper(rd, h, consensusAbi, consensusAddr)
            val n = (w.call("n")[0] as BigInteger).intValueExact()


            val li = mutableListOf<Pair<HexBytes, BigInteger>>()

            for (i in 1..n) {
                val addr = w.call("candidates", i.toBigInteger())[0] as ByteArray
                val votes = w.call("votes", addr)[0] as BigInteger
                li.add(Pair(addr.hex(), votes))
            }

            li.sortByDescending { it.second }
            li
        }
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
        val createCode = MapperUtil.OBJECT_MAPPER.readValue(FileUtils.getInputStream("pos-create-code.json"), JsonNode::class.java)
        val bin =  FileUtils.getInputStream("pos-abi.json").readAllBytes()
        consensusAbi = Abi.fromJson(String(bin))
        val c = createCode["object"]
        m[consensusAddr] = c.textValue().hex()
    }

    override val name: String
        get() = "pos"

    companion object {
        const val POS_INTERVAL = 120
        const val POS_MAX_CANDIDATES = 15
    }
}