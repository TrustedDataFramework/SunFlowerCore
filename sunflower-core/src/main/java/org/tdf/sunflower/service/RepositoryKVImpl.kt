package org.tdf.sunflower.service

import org.slf4j.LoggerFactory
import org.tdf.common.event.EventBus
import org.tdf.common.serialize.Codecs
import org.tdf.common.store.Store
import org.tdf.common.store.StoreWrapper
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.events.NewBestBlock
import org.tdf.sunflower.facade.DatabaseStoreFactory
import org.tdf.sunflower.facade.TransactionInfo
import org.tdf.sunflower.facade.blockHash
import org.tdf.sunflower.facade.index
import org.tdf.sunflower.state.AccountTrie
import org.tdf.sunflower.types.*
import java.util.*

class RepositoryKVImpl(
    bus: EventBus,
    factory: DatabaseStoreFactory,
    accountTrie: AccountTrie
) : AbstractRepository(
    bus, factory, accountTrie
) {
    // transaction hash -> transaction
    private val transactionsStore: Store<HexBytes, Transaction>

    // block hash -> header
    private val headerStore: Store<HexBytes, Header>

    // transactions root -> transaction hashes
    private val transactionsRoot: Store<HexBytes, Array<HexBytes>>

    // block height -> block hashes, the first hash is canonical
    private val heightIndex: Store<Long, Array<HexBytes>>

    // "best" -> best header, "prune" -> pruned header
    private val status: Store<String, Header>

    // transaction hash -> receipts
    private val transactionIndices: Store<HexBytes, Array<TransactionIndex>>


    override fun saveGenesis(b: Block) {
        super.saveGenesis(b)
        if (status[BEST_HEADER] == null) {
            status[BEST_HEADER] = b.header
        }
    }

    override fun getBlockFromHeader(header: Header): Block {
        val txHashes = transactionsRoot[header.transactionsRoot]
            ?: throw RuntimeException("transactions of header $header not found")
        return Block(header, txHashes.map { transactionsStore[it]!! })
    }

    override fun containsHeader(hash: HexBytes): Boolean {
        return headerStore[hash] != null
    }

    override val bestHeader: Header
        get() = status[BEST_HEADER]!!

    override fun getHeaderByHash(hash: HexBytes): Header? {
        return headerStore[hash]
    }

    override fun getHeadersBetween(startHeight: Long, stopHeight: Long, limit: Int, descend: Boolean): List<Header> {
        val ret: MutableList<Header> = mutableListOf()
        val range = if (descend) {
            stopHeight downTo startHeight
        } else {
            startHeight..stopHeight
        }

        for (i in range) {
            val idx = heightIndex[i] ?: continue
            for (bytes in idx) {
                val h = headerStore[bytes]!!
                ret.add(h)
                if (ret.size == limit)
                    return ret
            }
        }
        return ret
    }

    private fun getCanonicalHashAt(height: Long): HexBytes? {
        return heightIndex[height]?.getOrNull(0)
    }

    private fun setCanonicalHashAt(height: Long, hash: HexBytes) {
        val set = heightIndex[height]?.toMutableSet() ?: mutableSetOf()
        set.remove(hash)
        heightIndex[height] = arrayOf(hash) + set.toTypedArray()
    }

    override fun getHeadersByHeight(height: Long): List<Header> {
        return (heightIndex[height] ?: emptyArray()).map { getHeaderByHash(it)!! }
    }

    override fun writeBlock(b: Block, infos: List<TransactionInfo>) {
        writeBlockNoReset(b, infos)
        val best = bestBlock
        if (Block.BEST_COMPARATOR.compare(best, b) < 0) {
            status[BEST_HEADER] = b.header
            var hash = b.hash
            while (true) {
                // reset canonical hash
                val o = headerStore[hash] ?: break
                val canonicalHash = getCanonicalHashAt(o.height)
                if (canonicalHash == hash)
                    break
                setCanonicalHashAt(o.height, hash)
                hash = o.hashPrev
            }
            eventBus.publish(NewBestBlock(b))
        }
    }

    override fun containsTransaction(hash: HexBytes): Boolean {
        return transactionsStore[hash] != null
    }

    private fun isCanonical(h: Header): Boolean {
        val hash = getCanonicalHashAt(h.height)
        return hash?.equals(h.hash) ?: false
    }

    private fun isCanonical(hash: HexBytes): Boolean {
        val h = headerStore[hash] ?: return false
        return isCanonical(h)
    }

    override fun writeGenesis(genesis: Block) {
        writeBlockNoReset(genesis, emptyList())
        setCanonicalHashAt(0L, genesis.hash)
    }

    private fun writeBlockNoReset(block: Block, infos: List<TransactionInfo>) {
        // ensure the state root exists
        val v = accountTrie.trieStore[block.stateRoot.bytes]
        if (block.stateRoot != accountTrie.trie.nullHash
            && (v == null || v.isEmpty())
        ) {
            throw RuntimeException("unexpected error: account trie " + block.stateRoot + " not synced")
        }
        // if the block has written before
        if (containsHeader(block.hash)) return
        // write header into store
        headerStore[block.hash] = block.header
        // save transaction and transaction infos
        for (i in block.body.indices) {
            val t = block.body[i]
            // save transaction
            transactionsStore[t.hash] = t
            val info = infos[i]
            val found = transactionIndices[t.hash]
            val founds: MutableList<TransactionIndex> = found?.toMutableList() ?: mutableListOf()
            if (founds.none
                { it.blockHash == info.blockHash }
            ) {
                founds.add(info.index)
            }
            transactionIndices[t.hash] = founds.toTypedArray()
        }

        // save transaction root -> tx hashes
        val txHashes: Array<HexBytes> = block.body.map { it.hash }.toTypedArray()
        transactionsRoot[block.transactionsRoot] = txHashes

        // save header index
        val headerHashes: MutableList<HexBytes> = heightIndex[block.height]?.toMutableList() ?: mutableListOf()
        headerHashes.remove(block.hash)
        headerHashes.add(block.hash)
        heightIndex[block.height] = headerHashes.toTypedArray()

        log.info("write block at height " + block.height + " " + block.header.hash + " to database success")
    }

    override fun getCanonicalHeader(height: Long): Header? {
        val v = getCanonicalHashAt(height)
        return v?.let { getHeaderByHash(it) }
    }

    override fun getTransactionInfo(hash: HexBytes): TransactionInfo? {
        val infos = transactionIndices[hash] ?: emptyArray()
        val i = infos.firstOrNull { isCanonical(it.blockHash) } ?: return null
        return TransactionInfo(i, transactionsStore[hash]!!)
    }

    companion object {
        private const val BEST_HEADER = "best"
        private val log = LoggerFactory.getLogger("db")
    }

    init {
        transactionsStore = StoreWrapper(
            factory.create('b'),
            Codecs.hex,
            Transaction.Companion
        )

        headerStore = StoreWrapper(
            factory.create('h'),
            Codecs.hex,
            Header.Companion
        )
        transactionsRoot = StoreWrapper(
            factory.create('t'),
            Codecs.hex,
            Codecs.rlp(Array<HexBytes>::class.java)
        )
        heightIndex = StoreWrapper(
            factory.create('i'),
            Codecs.rlp(Long::class.java),
            Codecs.rlp(Array<HexBytes>::class.java)
        )
        status = StoreWrapper(
            factory.create('s'),
            Codecs.string,
            Header.Companion
        )
        transactionIndices = StoreWrapper(
            factory.create('f'),
            Codecs.hex,
            Codecs.rlp(Array<TransactionIndex>::class.java)
        )
    }
}