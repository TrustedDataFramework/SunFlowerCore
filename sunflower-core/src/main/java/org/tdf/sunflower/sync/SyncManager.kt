package org.tdf.sunflower.sync

import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.TickerMode
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.tdf.common.event.EventBus
import org.tdf.common.store.Store
import org.tdf.common.trie.Trie
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.AppConfig
import org.tdf.sunflower.SyncConfig
import org.tdf.sunflower.events.NewBlockMined
import org.tdf.sunflower.events.NewBlocksReceived
import org.tdf.sunflower.events.NewTransactionsCollected
import org.tdf.sunflower.facade.*
import org.tdf.sunflower.net.Context
import org.tdf.sunflower.net.Peer
import org.tdf.sunflower.net.PeerServer
import org.tdf.sunflower.state.Account
import org.tdf.sunflower.state.AccountTrie
import org.tdf.sunflower.state.StateTrie
import org.tdf.sunflower.types.*
import java.io.Closeable
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import javax.annotation.PostConstruct
import kotlin.math.abs

/**
 * sync manager for full-nodes
 */
@Component
class SyncManager(
    private val peerServer: PeerServer, private val engine: ConsensusEngine,
    private val repo: RepositoryService,
    private val transactionPool: TransactionPool, private val syncConfig: SyncConfig,
    private val eventBus: EventBus,
    accountTrie: AccountTrie,
    @Qualifier("contractStorageTrie") contractStorageTrie: Trie<HexBytes, HexBytes>,
    @Qualifier("contractCodeStore") contractCodeStore: Store<HexBytes, HexBytes>,
    miner: Miner
) : PeerServerListener, Closeable {
    private val mtx = Mutex()

    private val queue = TreeSet(Block.FAT_COMPARATOR)
    private val accountTrie: StateTrie<HexBytes, Account>
    private val fastSyncHeight: Long
    private val fastSyncHash: HexBytes
    private val contractStorageTrie: Trie<HexBytes, HexBytes>
    private val contractCodeStore: Store<HexBytes, HexBytes>
    private val miner: Miner
    private val limiters: Limiters
    private val receivedTransactions = CacheBuilder.newBuilder()
        .maximumSize(AppConfig.get().p2pTransactionCacheSize.toLong())
        .build<HexBytes, Boolean>()
    private val receivedProposals = CacheBuilder.newBuilder()
        .maximumSize(AppConfig.get().p2pProposalCacheSize.toLong())
        .build<HexBytes, Boolean>()


    // lock when accounts received, avoid concurrent handling
    private val fastSyncAddressesLock: Lock = ReentrantLock()

    @Volatile
    private var fastSyncBlock: Block? = null

    // lock when another node ask for all accounts in the trie, avoid concurrent traverse
    @Volatile
    private var trieTraverseLock = false

    // when fastSyncing = true, the node is in fast-syncing mode
    @Volatile
    private var fastSyncing = false

    @Volatile
    private var fastSyncTrie: Trie<HexBytes, Account>? = null

    @Volatile
    private var fastSyncTotalAccounts: Long = 0

    // not null when accounts transports, all accounts received when the size of this set == Accounts.getTotal()
    @Volatile
    private var fastSyncAddresses: MutableSet<HexBytes>? = null
    private fun isNotApproved(context: Context): Boolean {
        // filter nodes not auth
//        Optional<Set<HexBytes>> nodes = engine.getApprovedNodes();
//        if (nodes.isPresent() && !nodes.get().contains(Address.fromPublicKey(context.getRemote().getID()))) {
//            context.exit();
//            log.error("invalid node " + context.getRemote().getID() + " not approved");
//            return true;
//        }
        return false
    }

    private fun broadcastToApproved(body: ByteArray) {
        val peers = peerServer.peers
        for (p in peers) {
            peerServer.dial(p, body)
        }
    }

    @PostConstruct
    fun init() {
        // TODO: don't send status, proposal and transaction to peer not approved
        peerServer.addListeners(this)
        val writeTicker = ticker(TimeUnit.SECONDS.toMillis(syncConfig.blockWriteRate), mode = TickerMode.FIXED_DELAY)
        GlobalScope.launch {
            for (c in writeTicker) {
                tryWrite()
            }
        }
        val statusTicker = ticker(TimeUnit.SECONDS.toMillis(syncConfig.heartRate), mode = TickerMode.FIXED_DELAY)
        GlobalScope.launch {
            for (c in statusTicker) {
                sendStatus()
            }
        }

        eventBus.subscribe(NewBlockMined::class.java) { (block) ->
            broadcastToApproved(
                SyncMessage.encode(
                    SyncMessage.PROPOSAL, Proposal(
                        block
                    )
                )
            )
        }
        eventBus.subscribe(
            NewTransactionsCollected::class.java
        ) { e: NewTransactionsCollected ->
            broadcastToApproved(
                SyncMessage.encode(
                    SyncMessage.TRANSACTION,
                    e.transactions
                )
            )
        }
        eventBus.subscribe(NewBlocksReceived::class.java) { e: NewBlocksReceived -> onBlocks(e.blocks) }
    }

    private fun clearFastSyncCache() {
        fastSyncBlock = null
        fastSyncTrie = null
        fastSyncAddresses = null
    }

    override fun onMessage(context: Context, server: PeerServer) {
        GlobalScope.launch { repo.getWriter().use { writer -> onMessageInternal(context, server, writer) } }
    }

    private fun onMessageInternal(context: Context, server: PeerServer, writer: RepositoryReader) {
        val o = SyncMessage.decode(context.message)
        val bestBlock = writer.bestBlock
        if (!o.isPresent) return
        val msg = o.get()
        log.debug("receive {} message from {}", msg.code, context.remote)
        when (msg.code) {
            SyncMessage.UNKNOWN -> return
            SyncMessage.STATUS -> {
                if (limiters.status() != null && !limiters.status().tryAcquire()) {
                    log.error("receive status message too frequent")
                    return
                }
                val s = msg.getBodyAs(Status::class.java)
                onStatus(context, server, s, writer)
                return
            }
            SyncMessage.GET_BLOCKS -> {
                if (isNotApproved(context)) return
                if (limiters.blocks != null && !limiters.blocks.tryAcquire()) {
                    log.error("receive get-blocks message too frequent")
                    return
                }
                if (fastSyncing) return
                val getBlocks = msg.getBodyAs(GetBlocks::class.java)
                val blocks = writer.getBlocksBetween(
                    getBlocks.startHeight,
                    getBlocks.stopHeight,
                    Math.min(syncConfig.maxBlocksTransfer, getBlocks.limit),
                    getBlocks.isDescend
                )
                context.response(SyncMessage.encode(SyncMessage.BLOCKS, blocks))
                return
            }
            SyncMessage.TRANSACTION -> {
                if (isNotApproved(context)) return
                val txs = Arrays.asList(
                    *msg.getBodyAs(
                        Array<Transaction>::class.java
                    )
                )
                val root = Transaction.calcTxTrie(txs)
                if (receivedTransactions.asMap().containsKey(root)) return
                receivedTransactions.put(root, true)
                transactionPool.collect(txs)
                context.relay()
                return
            }
            SyncMessage.PROPOSAL -> {
                if (isNotApproved(context)) return
                val p = msg.getBodyAs(Proposal::class.java)
                val proposal = p.block ?: return
                if (fastSyncing && !receivedProposals.asMap().containsKey(proposal.hash)) {
                    receivedProposals.put(proposal.hash, true)
                    context.relay()
                    return
                }
                val best = writer.bestHeader
                if (receivedProposals.asMap().containsKey(proposal.hash)) {
                    return
                }
                receivedProposals.put(proposal.hash, true)
                context.relay()
                if (Math.abs(proposal.height - best.height) > syncConfig.maxPendingBlocks) {
                    return
                }
                if (writer.containsHeader(proposal.hash)) return
                mtx.tryLock()
                log.debug("lock acquired by thread ${Thread.currentThread().id} pos = 1")
                try {
                    queue.add(proposal)
                } finally {
                    log.debug("lock released by thread ${Thread.currentThread().id} pos = 1")
                    mtx.unlock()
                }
                return
            }
            SyncMessage.ACCOUNTS -> {

            }
            SyncMessage.BLOCKS -> {
                val blocks = msg.getBodyAs(
                    Array<Block>::class.java
                )
                log.debug(
                    "blocks received from start = ${blocks?.getOrNull(0)?.height} stop = ${
                        blocks?.getOrNull(
                            blocks.size - 1
                        )?.height
                    }"
                )
                onBlocks(blocks.asList())
                return
            }
        }
    }

    private fun onBlocks(blocks: List<Block>) {
        if (fastSyncing) {
            for (b in blocks) {
                b.resetTransactionsRoot()
                if (b.hash == fastSyncHash) {
                    fastSyncBlock = b
                    return
                }
            }
            return
        }
        if (!mtx.tryLock()) return
        log.debug("lock acquired by thread ${Thread.currentThread().id} pos = 2")
        try {
            repo.getReader().use { rd ->
                val best = rd.bestHeader
                val sorted = blocks.sortedWith(Block.FAT_COMPARATOR)
                for (block in sorted) {
                    if (queue.contains(block)) continue
                    //                if (block.getHeight() <= repository.getPrunedHeight())
//                    continue;
                    if (abs(block.height - best.height) > syncConfig.maxPendingBlocks) break
                    block.resetTransactionsRoot()
                    if (rd.containsHeader(block.hash)) continue
                    queue.add(block)
                }
            }
        } finally {
            log.debug("lock released by thread ${Thread.currentThread().id} pos = 2")
            mtx.unlock()
        }
    }

    //    private void finishFastSync() {
    //        this.fastSyncing = false;
    //        fastSyncTrie.flush();
    //        repository.writeBlock(fastSyncBlock);
    //        repository.prune(fastSyncBlock.getHash().getBytes());
    //        devAssert(
    //                repository.getPrunedHash().equals(fastSyncBlock.getHash()), "prune failed after fast sync");
    //        log.info("fast sync success to height {} hash {}", fastSyncHeight, fastSyncBlock.getHash());
    //        this.miner.start();
    //        clearFastSyncCache();
    //    }
    private fun onStatus(ctx: Context, server: PeerServer, s: Status, rd: RepositoryReader) {
        if (fastSyncing) {
            val fastSyncEnabled = (s.prunedHeight < fastSyncHeight
                    || s.prunedHash == fastSyncHash)
            if (!fastSyncEnabled && server.isFull) {
                log.info("cannot fast sync by peer " + ctx.remote + " block him")
                ctx.block()
                return
            }
            if (!fastSyncEnabled) return
            if (fastSyncBlock == null) {
                ctx.response(
                    SyncMessage.encode(
                        SyncMessage.GET_BLOCKS,
                        GetBlocks(fastSyncHeight, fastSyncHeight, false, syncConfig.maxBlocksTransfer)
                    )
                )
                log.info("fetch fast sync block at height {}", fastSyncHeight)
            }
            if (fastSyncBlock != null && fastSyncAddresses == null) {
                ctx.response(
                    SyncMessage.encode(
                        SyncMessage.GET_ACCOUNTS,
                        GetAccounts(fastSyncBlock!!.stateRoot, syncConfig.maxAccountsTransfer)
                    )
                )
                log.info("fast syncing: fetch accounts... ")
            }
            return
        }
        val best = rd.bestHeader
        var orphans: List<Block?> = emptyList()
        // try to sync orphans
        if (mtx.tryLock()) {
            log.debug("lock acquired by thread ${Thread.currentThread().id} pos = 3")
            orphans = try {
                getOrphansInternal(rd)
            } finally {
                log.debug("lock released by thread ${Thread.currentThread().id} = 3")
                mtx.unlock()
            }
        }
        for (b in orphans) {
            if (b != null && s.bestBlockHeight >= b.height && b.height > s.prunedHeight && !rd.containsHeader(b.hashPrev)) {
                SyncManager.log.debug("try to fetch orphans, head height {} hash {}", b.height, b.hash)
                // remote: prune < b <= best
                val getBlocks = GetBlocks(
                    s.prunedHeight, b.height, true,
                    syncConfig.maxBlocksTransfer
                ).clip()
                ctx.response(SyncMessage.encode(SyncMessage.GET_BLOCKS, getBlocks))
            }
        }
        if (s.bestBlockHeight >= best.height && s.bestBlockHash != best.hash) {
            val getBlocks = GetBlocks(
                Math.max(s.prunedHeight, best.height),
                s.bestBlockHeight, false,
                syncConfig.maxBlocksTransfer
            ).clip()
            log.debug("request for blocks start = ${getBlocks.startHeight} stop = ${getBlocks.stopHeight}")
            ctx.response(SyncMessage.encode(SyncMessage.GET_BLOCKS, getBlocks))
        }
    }

    private fun getOrphansInternal(rd: RepositoryReader): List<Block?> {
        val orphanHeads: MutableList<Block?> = ArrayList()
        val orphans: MutableSet<HexBytes> = HashSet()
        val noOrphans: MutableSet<HexBytes> = HashSet()
        for (block in queue) {
            if (noOrphans.contains(block.hashPrev)) {
                noOrphans.add(block.hash)
                continue
            }
            if (orphans.contains(block.hashPrev)) {
                orphans.add(block.hash)
                continue
            }
            if (rd.containsHeader(block.hashPrev)) {
                noOrphans.add(block.hash)
            } else {
                orphanHeads.add(block)
                orphans.add(block.hash)
            }
        }
        return orphanHeads
    }

    val orphans: List<Block>
        get() {
            if (!mtx.tryLock()) {
                throw RuntimeException("busy...")
            }
            log.debug("lock acquired by thread ${Thread.currentThread().id} = 4")
            try {
                repo.getReader().use { rd ->
                    val ret: MutableList<Block> = ArrayList()
                    val orphans: MutableSet<HexBytes> = HashSet()
                    val noOrphans: MutableSet<HexBytes> = HashSet()
                    for (block in queue) {
                        if (noOrphans.contains(block.hashPrev)) {
                            noOrphans.add(block.hash)
                            continue
                        }
                        if (orphans.contains(block.hashPrev)) {
                            orphans.add(block.hash)
                            ret.add(block)
                            continue
                        }
                        if (rd.containsHeader(block.hashPrev)) {
                            noOrphans.add(block.hash)
                        } else {
                            ret.add(block)
                            orphans.add(block.hash)
                        }
                    }
                    return ret
                }
            } finally {
                log.debug("lock released by thread ${Thread.currentThread().id} pos = 4")
                mtx.unlock()
            }
        }

    private fun tryWrite() {
        if (fastSyncing) return
        if (!mtx.tryLock()) return
        log.debug("lock acquired by thread ${Thread.currentThread().id} pos = 5")
        val it = queue.iterator()
        try {
            repo.getWriter().use { writer ->
                val best = writer.bestHeader
                val orphans: MutableSet<HexBytes> = HashSet()
                while (it.hasNext()) {
                    val b = it.next()
                    if (Math.abs(best.height - b.height) > syncConfig.maxAccountsTransfer //                        || b.getHeight() <= repository.getPrunedHeight()
                    ) {
                        it.remove()
                        continue
                    }
                    if (writer.containsHeader(b.hash)) {
                        it.remove()
                        continue
                    }
                    if (orphans.contains(b.hashPrev)) {
                        orphans.add(b.hash)
                        continue
                    }
                    val o = writer.getBlockByHash(b.hashPrev)
                    if (o == null) {
                        orphans.add(b.hash)
                        continue
                    }
                    val res = engine.validator.validate(b, o)
                    if (!res.isSuccess) {
                        it.remove()
                        log.error(res.reason)
                        continue
                    }
                    it.remove()
                    val rs = res as BlockValidateResult
                    writer.writeBlock(b, rs.infos)
                }
            }
        } finally {
            log.debug("lock released by thread ${Thread.currentThread().id} pos = 5")
            mtx.unlock()
        }
    }

    fun sendStatus() {
        if (fastSyncing) return
        repo.getReader().use { rd ->
            val best = rd.bestHeader
            val genesis = rd.genesis
            val status = Status(
                best.height,
                best.hash,
                genesis.hash,
                0,
                null //                repository.getPrunedHeight(),
                //                repository.getPrunedHash()
            )
            broadcastToApproved(SyncMessage.encode(SyncMessage.STATUS, status))
        }
    }

    override fun onStart(server: PeerServer) {}
    override fun onNewPeer(peer: Peer, server: PeerServer) {}
    override fun onDisconnect(peer: Peer, server: PeerServer) {}

    override fun close() {
        log.info("close sync manager")
    }

    init {
        //        this.fastSyncing = syncConfig.getFastSyncHeight() > 0
//            && repository.getBestHeader().getHeight() == 0;
        fastSyncHash = HexBytes.fromBytes(syncConfig.fastSyncHash)
        fastSyncHeight = syncConfig.fastSyncHeight
        this.accountTrie = accountTrie
        limiters = Limiters(syncConfig.rateLimits)
        this.contractStorageTrie = contractStorageTrie
        this.contractCodeStore = contractCodeStore
        this.miner = miner
        if (fastSyncing) this.miner.stop()
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger("sync")
    }
}