package org.tdf.sunflower.facade

import org.tdf.common.event.EventBus
import org.tdf.common.store.Store
import org.tdf.common.trie.Trie
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.state.Account
import org.tdf.sunflower.state.BuiltinContract
import org.tdf.sunflower.state.StateTrie
import org.tdf.sunflower.types.Block
import org.tdf.sunflower.types.ConsensusConfig

abstract class AbstractConsensusEngine : ConsensusEngine {
    // contract storage trie
    var contractStorageTrie: Trie<HexBytes, HexBytes>? = null

    // a map between hash and wasm byte code
    var contractCodeStore: Store<HexBytes, HexBytes>? = null

    // sub class should set miner explicitly when init() called
    private var miner: Miner? = null

    // sub class should set validator explicitly when init() called
    private var validator: Validator? = null

    // account stateTrie will be injected before init() called
    var accountTrie: StateTrie<HexBytes, Account>? = null

    // sub class should set genesis block explicitly when init() called
    private var genesisBlock: Block? = null

    // event bus will be injected before init() called
    var eventBus: EventBus? = null

    // transaction pool will be injected before init() called
    var transactionPool: TransactionPool? = null

    // sunflowerRepository will be injected before init() called
    var repo: RepositoryService? = null

    // sub class should set peer server listener explicitly when init() called
    private var peerServerListener = PeerServerListener.NONE
    open val alloc: List<Account?>?
        get() = emptyList()
    open val builtins: List<BuiltinContract?>?
        get() = emptyList()
    open val bios: List<BuiltinContract?>?
        get() = emptyList()

    override fun getMiner(): Miner {
        return miner!!
    }

    override fun getValidator(): Validator {
        return validator!!
    }

    override fun getGenesisBlock(): Block {
        return genesisBlock!!
    }

    override fun getPeerServerListener(): PeerServerListener {
        return peerServerListener
    }

    fun setMiner(miner: Miner?) {
        this.miner = miner
    }

    fun setValidator(validator: Validator?) {
        this.validator = validator
    }

    fun setGenesisBlock(genesisBlock: Block?) {
        this.genesisBlock = genesisBlock
    }

    fun setPeerServerListener(peerServerListener: PeerServerListener) {
        this.peerServerListener = peerServerListener
    }

    companion object {
        @JvmField
        val NONE: AbstractConsensusEngine = object : AbstractConsensusEngine() {
            override fun init(config: ConsensusConfig) {
                if (transactionPool == null) throw RuntimeException("transaction pool not injected")
                if (repo == null) throw RuntimeException("consortium repository not injected")
                if (eventBus == null) {
                    throw RuntimeException("event bus not injected")
                }
                setMiner(Miner.NONE)
                setValidator(Validator.NONE)
                setGenesisBlock(Block())
                peerServerListener = PeerServerListener.NONE
            }

            override fun getName(): String {
                return "none"
            }
        }
    }
}