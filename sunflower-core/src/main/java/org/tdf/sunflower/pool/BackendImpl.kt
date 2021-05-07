package org.tdf.sunflower.pool

import org.tdf.common.store.Store
import org.tdf.common.trie.Trie
import org.tdf.common.types.Uint256
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.state.Account
import org.tdf.sunflower.state.BuiltinContract
import org.tdf.sunflower.types.CryptoContext
import org.tdf.sunflower.types.Header
import org.tdf.sunflower.vm.Backend
import java.util.*

// backend for mining
class BackendImpl(
    private val parent: Header,
    override val parentBackend: BackendImpl? = null,
    private val trie: Trie<HexBytes, Account>,
    private val contractStorageTrie: Trie<HexBytes, HexBytes>,
    private val modifiedAccounts: MutableMap<HexBytes, Account> = mutableMapOf(),
    private val modifiedStorage: MutableMap<HexBytes, MutableMap<HexBytes, HexBytes>> = mutableMapOf(),
    override val builtins: Map<HexBytes, BuiltinContract> = mutableMapOf(),
    override val bios: Map<HexBytes, BuiltinContract> = mutableMapOf(),
    override val isStatic: Boolean,
    private val codeStore: Store<HexBytes, HexBytes>,
    private val codeCache: MutableMap<HexBytes, HexBytes> = mutableMapOf(),
    override var headerCreatedAt: Long? = null
) : Backend {

    private fun clearCache() {
        codeCache.clear()
        modifiedAccounts.clear()
        modifiedStorage.clear()
    }

    // get account by parent without clone
    private fun lookup(address: HexBytes): Account {
        if (parentBackend != null) return parentBackend.lookup(address)
        val a = trie[address]
        return a ?: Account.emptyAccount(address, Uint256.ZERO)
    }

    override fun createChild(): BackendImpl {
        return BackendImpl(
            parent,
            this,
            trie,
            contractStorageTrie,
            HashMap(),
            HashMap(),
            builtins,
            bios,
            isStatic,
            codeStore,
            codeCache,
            headerCreatedAt
        )
    }

    private fun mergeInternal(
        accounts: MutableMap<HexBytes, Account>,
        storage: MutableMap<HexBytes, MutableMap<HexBytes, HexBytes>>
    ) {
        parentBackend?.mergeInternal(accounts, storage)
        modifiedAccounts.forEach { (key: HexBytes, value: Account) -> accounts[key] = value }
        for ((key, value) in modifiedStorage) {
            storage.putIfAbsent(key, HashMap())
            storage[key]!!.putAll(value)
        }
    }

    override val trieRoot: HexBytes
        get() = trie.rootHash
    override val root: Backend
        get() = parentBackend?.root ?: this

    override fun merge(): HexBytes {
        val accounts: MutableMap<HexBytes, Account> = mutableMapOf()
        val storage: MutableMap<HexBytes, MutableMap<HexBytes, HexBytes>> = mutableMapOf()
        mergeInternal(accounts, storage)
        val modified: MutableSet<HexBytes> = mutableSetOf()
        modified.addAll(accounts.keys)
        modified.addAll(storage.keys)
        val tmpTrie = trie.revert(trie.rootHash)
        for (addr in modified) {
            var a = accounts[addr]
            // some account has not touched, but storage modified
            if (a == null) {
                a = lookup(addr)
            }
            val s = contractStorageTrie.revert(a.storageRoot)
            val map: Map<HexBytes, HexBytes> = storage.getOrDefault(addr, emptyMap())
            for ((key, value) in map) {
                if (value.size() == 0) {
                    s.remove(key)
                } else {
                    s.put(key, value)
                }
            }
            a.storageRoot = s.commit()
            tmpTrie.put(addr, a)
        }
        for ((key, value) in codeCache) {
            codeStore.put(key, value)
        }
        return tmpTrie.commit()
    }

    override val height: Long
        get() = parent.height + 1
    override val parentHash: HexBytes
        get() = parent.hash

    override fun getBalance(address: HexBytes): Uint256 {
        return if (modifiedAccounts.containsKey(address)) {
            modifiedAccounts[address]!!.balance
        } else lookup(address).balance
    }

    override fun setBalance(address: HexBytes, balance: Uint256?) {
        if (modifiedAccounts.containsKey(address)) {
            modifiedAccounts[address]!!.balance = balance
            return
        }
        val a = lookup(address).clone()
        a.balance = balance
        modifiedAccounts[address] = a
    }

    override fun getNonce(address: HexBytes): Long {
        return if (modifiedAccounts.containsKey(address)) {
            modifiedAccounts[address]!!.nonce
        } else lookup(address).nonce
    }

    override fun setNonce(address: HexBytes, nonce: Long) {
        if (modifiedAccounts.containsKey(address)) {
            modifiedAccounts[address]!!.nonce = nonce
            return
        }
        val a = lookup(address).clone()
        a.nonce = nonce
        modifiedAccounts[address] = a
    }

    override fun getInitialGas(payloadSize: Int): Long {
        return (payloadSize / 1024).toLong()
    }

    override fun dbSet(address: HexBytes, key: HexBytes, value: HexBytes) {
        if (key.size() == 0) throw RuntimeException("invalid key length = 0")
        modifiedStorage.putIfAbsent(address, HashMap())
        modifiedStorage[address]!![key] = value
    }

    override fun dbGet(address: HexBytes, key: HexBytes): HexBytes {
        if (key.size() == 0) throw RuntimeException("invalid key length = 0")

        // if has modified
        if (modifiedStorage.containsKey(address)) {
            // if it is delete mark
            val `val` = modifiedStorage[address]!![key]
            if (`val` != null) {
                // delete mark
                return `val`
            }
        }
        if (parentBackend != null) {
            return parentBackend.dbGet(address, key)
        }
        val a = trie[address] ?: return HexBytes.empty()
        val v = contractStorageTrie.revert(a.storageRoot)[key]
        return v ?: HexBytes.empty()
    }

    override fun dbHas(address: HexBytes, key: HexBytes): Boolean {
        if (key.size() == 0) throw RuntimeException("invalid key length = 0")
        val `val` = dbGet(address, key)
        return `val`.size() != 0
    }

    override fun getContractCreatedBy(address: HexBytes): HexBytes {
        return if (modifiedAccounts.containsKey(address)) {
            modifiedAccounts[address]!!.createdBy
        } else lookup(address).createdBy
    }

    override fun setContractCreatedBy(address: HexBytes, createdBy: HexBytes) {
        if (modifiedAccounts.containsKey(address)) {
            modifiedAccounts[address]!!.createdBy = createdBy
            return
        }
        val a = lookup(address).clone()
        a.createdBy = createdBy
        modifiedAccounts[address] = a
    }

    override fun dbRemove(address: HexBytes, key: HexBytes) {
        if (key.size() == 0) throw RuntimeException("invalid key length = 0")
        dbSet(address, key, HexBytes.empty())
    }

    override fun getCode(address: HexBytes): HexBytes {
        var a = modifiedAccounts[address]
        if (a == null) a = lookup(address).clone()
        if (a!!.contractHash == null || a.contractHash.size() == 0) return HexBytes.empty()
        var code = codeCache[a.contractHash]
        if (code != null && code.size() != 0) return code
        code = codeStore[a.contractHash]
        return code ?: HexBytes.empty()
    }

    override fun setCode(address: HexBytes, code: HexBytes) {
        val hash = CryptoContext.hash(code.bytes)
        if (modifiedAccounts.containsKey(address)) {
            modifiedAccounts[address]!!.contractHash = HexBytes.fromBytes(hash)
        } else {
            val a = lookup(address).clone()
            a.contractHash = HexBytes.fromBytes(hash)
            modifiedAccounts[address] = a
        }
        codeCache[HexBytes.fromBytes(hash)] = code
    }

    override fun onEvent(address: HexBytes, topics: List<Uint256>, data: ByteArray) {}
}