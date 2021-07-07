package org.tdf.sunflower.state

import org.slf4j.LoggerFactory
import org.tdf.common.serialize.Codecs
import org.tdf.common.store.NoDeleteStore
import org.tdf.common.store.Store
import org.tdf.common.trie.SecureTrie
import org.tdf.common.trie.Trie
import org.tdf.common.trie.TrieImpl
import org.tdf.common.util.ByteUtil
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.Start
import org.tdf.sunflower.types.Header
import org.tdf.sunflower.vm.Backend
import org.tdf.sunflower.vm.BackendImpl


class AccountTrie(
    override val db: Store<ByteArray, ByteArray>,
    private val contractCodeStore: Store<HexBytes, HexBytes>,
    val contractStorageTrie: Trie<HexBytes, HexBytes>,
    secure: Boolean
) : AbstractStateTrie<HexBytes, Account>() {
    override val trie: Trie<HexBytes, Account>

    var bios: Map<HexBytes, BuiltinContract> = emptyMap()
    var builtins: Map<HexBytes, BuiltinContract> = emptyMap()

    override val trieStore: Store<ByteArray, ByteArray>


    override fun createBackend(
        parent: Header,
        isStatic: Boolean,
        root: HexBytes,
    ): Backend {
        return BackendImpl(
            parent,
            null,
            trie.revert(root),
            contractStorageTrie,
            mutableMapOf(),
            mutableMapOf(),
            builtins,
            bios,
            isStatic,
            contractCodeStore,
            mutableMapOf(),
        )
    }

    init {
        trieStore = NoDeleteStore(db, ByteUtil::isNullOrZeroArray)
        var trie: Trie<HexBytes, Account> = TrieImpl(
            trieStore,
            Codecs.rlp(HexBytes::class.java),
            Codecs.rlp(Account::class.java),
        )
        if (secure)
            trie = SecureTrie(trie)
        this.trie = trie
    }

    override fun init(
        alloc: Map<HexBytes, Account>,
        bios: List<BuiltinContract>,
        builtins: List<BuiltinContract>
    ): HexBytes {
        this.builtins = builtins.associateBy { it.address }
        this.bios = bios.associateBy { it.address }

        val genesisStates: MutableMap<HexBytes, Account> = alloc.toMutableMap()

        for (c in (bios + builtins)) {
            val address = c.address

            val trie = contractStorageTrie.revert()
            for ((key, value) in c.genesisStorage) {
                trie[key] = value
            }
            val root = trie.commit()
            trie.flush()
            val a = Account(storageRoot = root)
            genesisStates[address] = a
        }

        // sync to genesis
        val tmp = trie.revert()
        genesisStates.forEach { (k: HexBytes, v: Account) -> tmp[k] = v }
        log.info("genesis states = {}", Start.MAPPER.writeValueAsString(genesisStates))
        val r = tmp.commit()
        log.info("genesis state root = $r")
        tmp.flush()
        return r
    }


    companion object {
        private val log = LoggerFactory.getLogger("trie")
    }
}