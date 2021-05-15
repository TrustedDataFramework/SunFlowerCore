package org.tdf.sunflower.vm

import org.tdf.common.store.Store
import org.tdf.common.trie.Trie
import org.tdf.common.types.Uint256
import org.tdf.common.util.HashUtil
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.state.Account
import org.tdf.sunflower.state.BuiltinContract
import org.tdf.sunflower.types.CryptoContext
import org.tdf.sunflower.types.Header

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

    // address -> code
    private val codeStore: Store<HexBytes, HexBytes>,
    private val codeCache: MutableMap<HexBytes, HexBytes> = mutableMapOf(),
    override var headerCreatedAt: Long? = null
) : Backend {

    private fun clearCache() {
        codeCache.clear()
        modifiedAccounts.clear()
        modifiedStorage.clear()
    }

    // get account without clone
    private fun lookup(address: HexBytes): Account {
        val a = modifiedAccounts[address]
        if (a != null)
            return a
        if (parentBackend != null) return parentBackend.lookup(address)
        val aInTrie = trie[address]
        return aInTrie ?: Account.emptyAccount(address, Uint256.ZERO)
    }

    override fun createChild(): BackendImpl {
        return BackendImpl(
            parent,
            this,
            trie,
            contractStorageTrie,
            mutableMapOf(),
            mutableMapOf(),
            builtins,
            bios,
            isStatic,
            codeStore,
            mutableMapOf(),
            headerCreatedAt
        )
    }

    private fun mergeInternal(
        accounts: MutableMap<HexBytes, Account>,
        storage: MutableMap<HexBytes, MutableMap<HexBytes, HexBytes>>,
        code: MutableMap<HexBytes, HexBytes>
    ) {
        parentBackend?.mergeInternal(accounts, storage, code)
        modifiedAccounts.forEach { (key: HexBytes, value: Account) -> accounts[key] = value }
        codeCache.forEach { (key: HexBytes, value: HexBytes) -> code[key] = value }
        for ((key, value) in modifiedStorage) {
            storage.putIfAbsent(key, mutableMapOf())
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
        val code = mutableMapOf<HexBytes, HexBytes>()
        mergeInternal(accounts, storage, code)
        val modified: MutableSet<HexBytes> = mutableSetOf()
        modified.addAll(accounts.keys)
        modified.addAll(storage.keys)
        modified.addAll(code.keys)
        val tmpTrie = trie.revert(trie.rootHash)
        for (addr in modified) {
            var a = accounts[addr]
            // some account has not touched, but storage or code modified
            if (a == null) {
                a = lookup(addr)
            }
            val s = contractStorageTrie.revert(a.storageRoot)
            val map: Map<HexBytes, HexBytes> = storage.getOrDefault(addr, emptyMap())
            for ((key, value) in map) {
                if (value.size() == 0) {
                    s.remove(key)
                } else {
                    s[key] = value
                }
            }
            a.storageRoot = s.commit()
            tmpTrie[addr] = a
        }
        for ((key, value) in codeCache) {
            codeStore[
                    tmpTrie[key]!!.contractHash
            ] = value
        }
        return tmpTrie.commit()
    }

    override fun close() {

    }

    override fun getContractHash(address: HexBytes): HexBytes {
        return lookup(address).contractHash
    }

    override val height: Long
        get() = parent.height + 1
    override val parentHash: HexBytes
        get() = parent.hash

    override fun getBalance(address: HexBytes): Uint256 {
        return lookup(address).balance
    }

    override fun setBalance(address: HexBytes, balance: Uint256?) {
        val a = lookup(address).clone()
        a.balance = balance
        modifiedAccounts[address] = a
    }

    override fun getNonce(address: HexBytes): Long {
        return lookup(address).nonce
    }

    override fun setNonce(address: HexBytes, nonce: Long) {
        val a = lookup(address).clone()
        a.nonce = nonce
        modifiedAccounts[address] = a
    }

    override fun dbSet(address: HexBytes, key: HexBytes, value: HexBytes) {
        if (key.size() == 0) throw RuntimeException("invalid key length = 0")
        modifiedStorage.putIfAbsent(address, mutableMapOf())
        modifiedStorage[address]!![key] = value
    }

    override fun dbGet(address: HexBytes, key: HexBytes): HexBytes {
        if (key.size() == 0) throw RuntimeException("invalid key length = 0")

        // if has modified
        if (modifiedStorage.containsKey(address)) {
            // if it is delete mark
            val value = modifiedStorage[address]!![key]
            if (value != null) {
                // delete mark
                return value
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
        val value = dbGet(address, key)
        return value.size() != 0
    }

    override fun getContractCreatedBy(address: HexBytes): HexBytes {
        return lookup(address).createdBy
    }

    override fun setContractCreatedBy(address: HexBytes, createdBy: HexBytes) {
        val a = lookup(address).clone()
        a.createdBy = createdBy
        modifiedAccounts[address] = a
    }

    override fun dbRemove(address: HexBytes, key: HexBytes) {
        if (key.size() == 0) throw RuntimeException("invalid key length = 0")
        dbSet(address, key, HexBytes.empty())
    }

    private fun lookupCode(address: HexBytes): HexBytes {
        val h = codeCache[address]
        if (h != null)
            return h
        if (parentBackend != null)
            return parentBackend.lookupCode(address)
        val a = lookup(address)
        val code = codeStore[a.contractHash]
        return code ?: HexBytes.empty()
    }

    override fun getCode(address: HexBytes): HexBytes {
        val a = lookup(address)
        if (a.contractHash == HashUtil.EMPTY_DATA_HASH_HEX)
            return HexBytes.empty()
        return lookupCode(a.address)
    }

    override fun setCode(address: HexBytes, code: HexBytes) {
        val hash = CryptoContext.hash(code.bytes)
        val a = lookup(address).clone()
        a.contractHash = HexBytes.fromBytes(hash)
        modifiedAccounts[address] = a
        codeCache[address] = code
    }

    override fun onEvent(address: HexBytes, topics: List<Uint256>, data: ByteArray) {}
}