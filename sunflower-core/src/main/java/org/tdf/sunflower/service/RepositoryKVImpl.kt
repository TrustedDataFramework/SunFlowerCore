package org.tdf.sunflower.service

import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.tdf.common.serialize.Codecs
import org.tdf.common.store.Store
import org.tdf.common.store.StoreWrapper
import org.tdf.common.util.FastByteComparisons
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.events.NewBestBlock
import org.tdf.sunflower.types.Block
import org.tdf.sunflower.types.Header
import org.tdf.sunflower.types.Transaction
import org.tdf.sunflower.types.TransactionInfo
import java.util.*

class RepositoryKVImpl(context: ApplicationContext) : AbstractRepository(
    context
) {
    // transaction hash -> transaction
    private val transactionsStore: Store<HexBytes, Transaction>

    // block hash -> header
    private val headerStore: Store<HexBytes, Header>

    // transactions root -> transaction hashes
    private val transactionsRoot: Store<HexBytes, Array<HexBytes>>

    // block height -> block hashes
    private val heightIndex: Store<Long, Array<HexBytes>>

    // block height -> canonical hash
    private val canonicalIndex: Store<Long, HexBytes>

    // "best" -> best header, "prune" -> pruned header
    private val status: Store<String, Header>

    // transaction hash -> receipts
    private val transactionInfos: Store<HexBytes, Array<TransactionInfo>>


    override fun saveGenesis(b: Block) {
        super.saveGenesis(b)
        if (status[BEST_HEADER] == null) {
            status.put(BEST_HEADER, b.header)
        }
    }

    override fun getBlockFromHeader(header: Header): Block {
        val txHashes = transactionsRoot[header.transactionsRoot]
            ?: throw RuntimeException("transactions of header $header not found")
        val body: MutableList<Transaction> = ArrayList(txHashes.size)
        for (hash in txHashes) {
            val t = transactionsStore[hash] ?: throw RuntimeException("transaction $hash not found")
            body.add(t)
        }
        val ret = Block(header)
        ret.body = body
        return ret
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
        val ret: MutableList<Header> = ArrayList()
        val range = if (descend) {
            stopHeight downTo startHeight
        } else {
            startHeight..stopHeight
        }

        for (i in range) {
            val idx = heightIndex[i] ?: continue
            for (bytes in idx) {
                val h = headerStore[bytes] ?: continue
                ret.add(h)
            }
            if (ret.size > limit) break
        }
        return ret
    }

    override fun getHeadersByHeight(height: Long): List<Header> {
        val idx = heightIndex[height] ?: return emptyList()
        val ret: MutableList<Header> = ArrayList(idx.size)
        for (bytes in idx) {
            val h = headerStore[bytes] ?: continue
            ret.add(h)
        }
        return ret
    }

    override fun writeBlock(b: Block, infos: List<TransactionInfo>) {
        writeBlockNoReset(b, infos)
        val best = bestBlock
        if (Block.BEST_COMPARATOR.compare(best, b) < 0) {
            status.put(BEST_HEADER, b.header)
            var hash = b.hash
            while (true) {
                val o = headerStore[hash] ?: break
                val canonicalHash = canonicalIndex[o.height]
                if (canonicalHash != null && canonicalHash.size() != 0 && canonicalHash == hash) break
                canonicalIndex.put(o.height, hash)
                hash = o.hashPrev
            }
            eventBus.publish(NewBestBlock(b))
        }
    }

    override fun containsTransaction(hash: HexBytes): Boolean {
        return transactionsStore[hash] != null
    }

    private fun isCanonical(h: Header): Boolean {
        val hash = canonicalIndex[h.height]
        return if (hash == null || hash.size() == 0) false else hash == h.hash
    }

    private fun isCanonical(hash: HexBytes): Boolean {
        val h = headerStore[hash] ?: return false
        return isCanonical(h)
    }

    override fun writeGenesis(genesis: Block) {
        writeBlockNoReset(genesis, emptyList())
        canonicalIndex.put(0L, genesis.hash)
    }

    private fun writeBlockNoReset(block: Block, infos: List<TransactionInfo>) {
        // ensure the state root exists
        val v = accountTrie!!.trieStore[block.stateRoot.bytes]
        if (block.stateRoot != accountTrie!!.trie.nullHash
            && Store.IS_NULL.test(v)
        ) {
            throw RuntimeException("unexpected error: account trie " + block.stateRoot + " not synced")
        }
        // if the block has written before
        if (containsHeader(block.hash)) return
        // write header into store
        headerStore.put(block.hash, block.header)
        // save transaction and transaction infos
        for (i in block.body.indices) {
            val t = block.body[i]
            // save transaction
            transactionsStore.put(t.hashHex, t)
            val info = infos[i]
            val found = transactionInfos[t.hashHex]
            val founds: MutableList<TransactionInfo> = found?.toMutableList() ?: mutableListOf()
            if (founds.none
                { FastByteComparisons.equal(it.blockHash, info.blockHash) }
            ) {
                founds.add(info)
            }
            transactionInfos.put(t.hashHex, founds.toTypedArray())
        }

        // save transaction root -> tx hashes
        val txHashes: Array<HexBytes> = block.body.map { it.hashHex }.toTypedArray()
        transactionsRoot.put(
            block.transactionsRoot,
            txHashes
        )

        val headerHashes: MutableSet<HexBytes> = heightIndex[block.height]?.toMutableSet() ?: mutableSetOf()
        headerHashes.add(block.hash)
        heightIndex.put(block.height, headerHashes.toTypedArray())
        log.info("write block at height " + block.height + " " + block.header.hash + " to database success")
    }

    override fun getCanonicalHeader(height: Long): Header? {
        val v = canonicalIndex[height]
        return if (v == null || v.size() == 0) null else getHeaderByHash(v)
    }

    override fun getTransactionInfo(hash: HexBytes): TransactionInfo? {
        val infos = transactionInfos[hash]
        if (infos == null || infos.isEmpty())
            return null
        for (info in infos) {
            headerStore[HexBytes.fromBytes(info.blockHash)] ?: continue
            if (isCanonical(HexBytes.fromBytes(info.blockHash))) {
                info.receipt.transaction = transactionsStore[hash]
            }
        }
        return null
    }

    companion object {
        private const val BEST_HEADER = "best"
        private val log = LoggerFactory.getLogger("db");
    }

    init {
        transactionsStore = StoreWrapper<HexBytes, Transaction, Any, Any>(
            factory.create("transactions"),
            Codecs.HEX,
            Codecs.newRLPCodec(Transaction::class.java)
        )
        headerStore = StoreWrapper<HexBytes, Header, Any, Any>(
            factory.create("headers"),
            Codecs.HEX,
            Codecs.newRLPCodec(Header::class.java)
        )
        transactionsRoot = StoreWrapper<HexBytes, Array<HexBytes>, Any, Any>(
            factory.create("transactions-root"),
            Codecs.HEX,
            Codecs.newRLPCodec(Array<HexBytes>::class.java)
        )
        heightIndex = StoreWrapper<Long, Array<HexBytes>, Any, Any>(
            factory.create("height-index"),
            Codecs.newRLPCodec(Long::class.java),
            Codecs.newRLPCodec(Array<HexBytes>::class.java)
        )
        status = StoreWrapper<String, Header, Any, Any>(
            factory.create("block-store-status"),
            Codecs.STRING,
            Codecs.newRLPCodec(Header::class.java)
        )
        canonicalIndex = StoreWrapper<Long, HexBytes, Any, Any>(
            factory.create("canonical-index"),
            Codecs.newRLPCodec(Long::class.java),
            Codecs.HEX
        )
        transactionInfos = StoreWrapper<HexBytes, Array<TransactionInfo>, Any, Any>(
            factory.create("transaction-infos"),
            Codecs.HEX,
            Codecs.newRLPCodec(Array<TransactionInfo>::class.java)
        )
    }
}