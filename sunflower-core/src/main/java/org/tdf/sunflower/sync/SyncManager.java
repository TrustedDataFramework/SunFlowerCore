package org.tdf.sunflower.sync;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.tdf.common.event.EventBus;
import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.FastByteComparisons;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.AppConfig;
import org.tdf.sunflower.ApplicationConstants;
import org.tdf.sunflower.SyncConfig;
import org.tdf.sunflower.events.*;
import org.tdf.sunflower.facade.*;
import org.tdf.sunflower.net.Context;
import org.tdf.sunflower.net.Peer;
import org.tdf.sunflower.net.PeerServer;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.AccountTrie;
import org.tdf.sunflower.state.Address;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.*;

import javax.annotation.PostConstruct;
import java.io.Closeable;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * sync manager for full-nodes
 */
@Component
@Slf4j(topic = "sync")
public class SyncManager implements PeerServerListener, Closeable {
    private final PeerServer peerServer;
    private final ConsensusEngine engine;
    private final SunflowerRepository repository;
    private final TransactionPool transactionPool;
    private final TreeSet<Block> queue = new TreeSet<>(Block.FAT_COMPARATOR);
    private final SyncConfig syncConfig;
    private final EventBus eventBus;
    private final StateTrie<HexBytes, Account> accountTrie;
    private final long fastSyncHeight;
    private final HexBytes fastSyncHash;
    private final Trie<HexBytes, HexBytes> contractStorageTrie;
    private final Store<HexBytes, HexBytes> contractCodeStore;
    private final Miner miner;

    private final Limiters limiters;
    private final Cache<HexBytes, Boolean> receivedTransactions = CacheBuilder.newBuilder()
            .maximumSize(AppConfig.get().getP2pTransactionCacheSize())
            .build();
    private final Cache<HexBytes, Boolean> receivedProposals = CacheBuilder.newBuilder()
            .maximumSize(AppConfig.get().getP2pProposalCacheSize())
            .build();
    private final Lock blockQueueLock = new ReentrantLock();
    // lock when accounts received, avoid concurrent handling
    private final Lock fastSyncAddressesLock = new ReentrantLock();
    private final ScheduledExecutorService executorService;
    private volatile Block fastSyncBlock;
    // lock when another node ask for all accounts in the trie, avoid concurrent traverse
    private volatile boolean trieTraverseLock;
    // when fastSyncing = true, the node is in fast-syncing mode
    private volatile boolean fastSyncing;
    private volatile Trie<HexBytes, Account> fastSyncTrie;
    private volatile long fastSyncTotalAccounts;

    // not null when accounts transports, all accounts received when the size of this set == Accounts.getTotal()
    private volatile Set<HexBytes> fastSyncAddresses;

    public SyncManager(
            PeerServer peerServer, ConsensusEngine engine,
            SunflowerRepository repository,
            TransactionPool transactionPool, SyncConfig syncConfig,
            EventBus eventBus,
            AccountTrie accountTrie,
            @Qualifier("contractStorageTrie") Trie<HexBytes, HexBytes> contractStorageTrie,
            @Qualifier("contractCodeStore") Store<HexBytes, HexBytes> contractCodeStore,
            Miner miner
    ) {
        this.peerServer = peerServer;
        this.engine = engine;
        this.repository = repository;
        this.transactionPool = transactionPool;
        this.syncConfig = syncConfig;
        this.eventBus = eventBus;
        this.fastSyncing = syncConfig.getFastSyncHeight() > 0
                && repository.getBestHeader().getHeight() == 0;
        this.fastSyncHash = HexBytes.fromBytes(syncConfig.getFastSyncHash());
        this.fastSyncHeight = syncConfig.getFastSyncHeight();
        this.accountTrie = accountTrie;
        this.limiters = new Limiters(syncConfig.getRateLimits());
        this.contractStorageTrie = contractStorageTrie;
        this.contractCodeStore = contractCodeStore;
        this.miner = miner;
        int core = Runtime.getRuntime().availableProcessors();

        executorService = Executors.newScheduledThreadPool(
                core > 1 ? core / 2 : core,
                new ThreadFactoryBuilder().setNameFormat("sync-manger-thread-%d").build()
        );
        if (this.fastSyncing)
            this.miner.stop();
    }

    private boolean isNotApproved(Context context) {
        // filter nodes not auth
//        Optional<Set<HexBytes>> nodes = engine.getApprovedNodes();
//        if (nodes.isPresent() && !nodes.get().contains(Address.fromPublicKey(context.getRemote().getID()))) {
//            context.exit();
//            log.error("invalid node " + context.getRemote().getID() + " not approved");
//            return true;
//        }
        return false;
    }

    private void broadcastToApproved(byte[] body) {
        List<Peer> peers = peerServer.getPeers();

        for (Peer p : peers) {
            peerServer.dial(p, body);
        }
    }

    @PostConstruct
    public void init() {
        // TODO: don't send status, proposal and transaction to peer not approved
        peerServer.addListeners(this);
        executorService.scheduleWithFixedDelay(
                this::tryWrite, 0,
                syncConfig.getBlockWriteRate(), TimeUnit.SECONDS);

        executorService.scheduleWithFixedDelay(
                this::sendStatus, 0,
                syncConfig.getHeartRate(), TimeUnit.SECONDS
        );
        eventBus.subscribe(NewBlockMined.class, (e) -> broadcastToApproved(SyncMessage.encode(SyncMessage.PROPOSAL, new Proposal(e.getBlock()))));
        eventBus.subscribe(NewTransactionsCollected.class,
                (e) -> broadcastToApproved(SyncMessage.encode(SyncMessage.TRANSACTION, e.getTransactions()))
        );
        eventBus.subscribe(NewBlocksReceived.class, (e) -> onBlocks(e.getBlocks()));
    }

    private void clearFastSyncCache() {
        this.fastSyncBlock = null;
        this.fastSyncTrie = null;
        this.fastSyncAddresses = null;
    }

    @Override
    public void onMessage(Context context, PeerServer server) {
        executorService.execute(() -> onMessageInternal(context, server));
    }

    @SneakyThrows
    private void onMessageInternal(Context context, PeerServer server) {
        Optional<SyncMessage> o = SyncMessage.decode(context.getMessage());
        Block bestBlock = repository.getBestBlock();
        ;
        if (!o.isPresent()) return;
        SyncMessage msg = o.get();
        log.debug("receive {} message from {}", msg.getCode(), context.getRemote());
        switch (msg.getCode()) {
            case SyncMessage.UNKNOWN:
                return;
            case SyncMessage.STATUS: {
                if (limiters.status() != null && !limiters.status().tryAcquire()) {
                    log.error("receive status message too frequent");
                    return;
                }
                Status s = msg.getBodyAs(Status.class);
                this.onStatus(context, server, s);
                return;
            }
            case SyncMessage.GET_BLOCKS: {
                if (isNotApproved(context))
                    return;
                if (limiters.getBlocks() != null && !limiters.getBlocks().tryAcquire()) {
                    log.error("receive get-blocks message too frequent");
                    return;
                }
                if (fastSyncing) return;
                GetBlocks getBlocks = msg.getBodyAs(GetBlocks.class);
                List<Block> blocks = repository.getBlocksBetween(
                        getBlocks.getStartHeight(),
                        getBlocks.getStopHeight(),
                        Math.min(syncConfig.getMaxBlocksTransfer(), getBlocks.getLimit()),
                        getBlocks.isDescend()
                );
                context.response(SyncMessage.encode(SyncMessage.BLOCKS, blocks));
                return;
            }
            case SyncMessage.TRANSACTION: {
                if (isNotApproved(context))
                    return;
                List<Transaction> txs = Arrays.asList(msg.getBodyAs(Transaction[].class));
                HexBytes root = Transaction.calcTxTrie(txs);
                if (receivedTransactions.asMap().containsKey(root))
                    return;
                receivedTransactions.put(root, true);
                transactionPool.collect(txs);
                context.relay();
                return;
            }
            case SyncMessage.PROPOSAL: {
                if (isNotApproved(context))
                    return;
                Proposal p = msg.getBodyAs(Proposal.class);
                Block proposal = p.getBlock();
                if (proposal == null)
                    return;
                if (fastSyncing && !receivedProposals.asMap().containsKey(proposal.getHash())) {
                    receivedProposals.put(proposal.getHash(), true);
                    context.relay();
                    return;
                }
                Header best = repository.getBestHeader();
                if (receivedProposals.asMap().containsKey(proposal.getHash())) {
                    return;
                }
                receivedProposals.put(proposal.getHash(), true);
                context.relay();
                if (Math.abs(proposal.getHeight() - best.getHeight()) > syncConfig.getMaxPendingBlocks()) {
                    return;
                }
                if (repository.containsHeader(proposal.getHash().getBytes()))
                    return;
                if (!blockQueueLock.tryLock(syncConfig.getLockTimeout(), TimeUnit.SECONDS))
                    return;
                try {
                    queue.add(proposal);
                } finally {
                    blockQueueLock.unlock();
                }
                return;
            }
//            case SyncMessage.GET_ACCOUNTS: {
//                if (isNotApproved(context))
//                    return;
//                if (fastSyncing || this.trieTraverseLock) return;
//                this.trieTraverseLock = true;
//                CompletableFuture.runAsync(() -> {
//                    try {
//                        GetAccounts getAccounts = msg.getBodyAs(GetAccounts.class);
//                        Trie<HexBytes, Account> trie = accountTrie.getTrie(getAccounts.getStateRoot().getBytes());
//                        int[] total = new int[1];
//                        List<SyncAccount> accounts = new ArrayList<>();
//                        trie.traverse((e) -> {
//                            Account a = e.getValue();
//                            List<byte[]> kv = new ArrayList<>();
//                            if (a.getStorageRoot() != null && a.getStorageRoot().length != 0) {
//                                contractStorageTrie.revert(a.getStorageRoot())
//                                        .forEach((k, v) -> {
//                                            kv.add(k);
//                                            kv.add(v);
//                                        });
//                            }
//                            accounts.add(new SyncAccount(
//                                    a,
//                                    (a.getContractHash() == null || a.getContractHash().length == 0) ?
//                                            null :
//                                            contractCodeStore.get(a.getContractHash()).get(),
//                                    kv
//                            ));
//                            total[0]++;
//                            if (accounts.size() % Math.min(syncConfig.getMaxAccountsTransfer(), getAccounts.getMaxAccounts()) == 0) {
//                                context.response(
//                                        SyncMessage.encode(SyncMessage.ACCOUNTS, new Accounts(0, accounts, false))
//                                );
//                                log.info("{} accounts traversed", total[0]);
//                                accounts.clear();
//                            }
//                            return true;
//                        });
//                        context.response(SyncMessage.encode(SyncMessage.ACCOUNTS, new Accounts(total[0], accounts, true)));
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    } finally {
//                        this.trieTraverseLock = false;
//                    }
//                });
//                return;
//            }
            case SyncMessage.ACCOUNTS: {
                if (!fastSyncing) return;
                if (fastSyncAddresses == null) {
                    fastSyncAddresses = new HashSet<>();
                }
                if (fastSyncTrie == null) {
//                    fastSyncTrie = accountTrie.getTrie().revert(
//                            accountTrie.getTrie().getNullHash(),
//                            new CachedStore<>(accountTrie.getTrieStore(), ByteArrayMap::new)
//                    );
                }
                this.fastSyncAddressesLock.lock();
                try {
                    Accounts accounts = msg.getBodyAs(Accounts.class);
                    for (SyncAccount sa : accounts.getAccounts()) {
                        Account a = sa.getAccount();
                        if (this.fastSyncAddresses.contains(a.getAddress()))
                            continue;
                        // validate contract code
                        if (a.getContractHash() != null && a.getContractHash().size() != 0) {
                            HexBytes key = HexBytes.fromBytes(CryptoContext.hash(sa.getContractCode().getBytes()));
                            if (!key.equals(a.getContractHash())) {
                                log.error("contract hash not match");
                                continue;
                            }
                            contractCodeStore.put(key, sa.getContractCode());
                        }

                        // validate storage root
                        if (a.getStorageRoot() != null && a.getStorageRoot().size() != 0) {
                            Trie<HexBytes, HexBytes> empty = contractStorageTrie.revert();
                            for (int i = 0; i < sa.getContractStorage().size() / 2; i += 1) {
                                HexBytes k = sa.getContractStorage().get(2 * i);
                                HexBytes v = sa.getContractStorage().get(2 * i + 1);
                                empty.put(k, v);
                            }

                            HexBytes root = empty.commit();
                            if (!root.equals(a.getStorageRoot())) {
                                log.error("storage root not match");
                                continue;
                            }
                        }

                        fastSyncAddresses.add(a.getAddress());
                        fastSyncTrie.put(a.getAddress(), a);
                    }
                    log.info("synced accounts = " + fastSyncAddresses.size());
                    if (accounts.isTraversed()) {
                        fastSyncTotalAccounts = accounts.getTotal();
                    }
                    if (fastSyncAddresses.size() != fastSyncTotalAccounts) {
                        return;
                    }
                    HexBytes stateRoot =fastSyncTrie.commit();
                    if (!fastSyncBlock.getStateRoot().equals(stateRoot)) {
                        clearFastSyncCache();
                        log.error("fast sync failed, state root not match, malicious node may exists in network!!!");
                        return;
                    }
//                    finishFastSync();
                } finally {
                    this.fastSyncAddressesLock.unlock();
                }
                return;
            }

            case SyncMessage.BLOCKS: {
                Block[] blocks = msg.getBodyAs(Block[].class);
                onBlocks(Arrays.asList(blocks));
                return;
            }
        }
    }

    @SneakyThrows
    private void onBlocks(List<Block> blocks) {
        if (fastSyncing) {
            for (Block b : blocks) {
                b.resetTransactionsRoot();
                if (b.getHash().equals(fastSyncHash)) {
                    this.fastSyncBlock = b;
                    return;
                }
            }
            return;
        }
        Header best = repository.getBestHeader();
        blocks.sort(Block.FAT_COMPARATOR);
        if (!blockQueueLock.tryLock(syncConfig.getLockTimeout(), TimeUnit.SECONDS))
            return;
        try {
            for (Block block : blocks) {
                if (queue.contains(block))
                    continue;
//                if (block.getHeight() <= repository.getPrunedHeight())
//                    continue;
                if (Math.abs(block.getHeight() - best.getHeight()) > syncConfig.getMaxPendingBlocks())
                    break;
                block.resetTransactionsRoot();
                if (repository.containsHeader(block.getHash().getBytes()))
                    continue;
                queue.add(block);
            }
        } finally {
            blockQueueLock.unlock();
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

    @SneakyThrows
    private void onStatus(Context ctx, PeerServer server, Status s) {
        if (fastSyncing) {
            boolean fastSyncEnabled =
                    s.getPrunedHeight() < fastSyncHeight
                            || s.getPrunedHash().equals(fastSyncHash);
            if (!fastSyncEnabled && server.isFull()) {
                log.info("cannot fast sync by peer " + ctx.getRemote() + " block him");
                ctx.block();
                return;
            }
            if (!fastSyncEnabled) return;
            if (fastSyncBlock == null) {
                ctx.response(
                        SyncMessage.encode(
                                SyncMessage.GET_BLOCKS,
                                new GetBlocks(fastSyncHeight, fastSyncHeight, false, syncConfig.getMaxBlocksTransfer())
                        ));
                log.info("fetch fast sync block at height {}", fastSyncHeight);
            }
            if (fastSyncBlock != null && fastSyncAddresses == null) {
                ctx.response(SyncMessage.encode(
                        SyncMessage.GET_ACCOUNTS,
                        new GetAccounts(fastSyncBlock.getStateRoot(), syncConfig.getMaxAccountsTransfer())
                ));
                log.info("fast syncing: fetch accounts... ");
            }
            return;
        }
        Header best = repository.getBestHeader();
        List<Block> orphans = Collections.emptyList();
        // try to sync orphans
        if (blockQueueLock.tryLock(syncConfig.getLockTimeout(), TimeUnit.SECONDS)) {
            try {
                orphans = getOrphansInternal();
            } finally {
                blockQueueLock.unlock();
            }
        }

        for (Block b : orphans) {
            if (b != null
                    && s.getBestBlockHeight() >= b.getHeight()
                    && b.getHeight() > s.getPrunedHeight()
                    && !repository.containsHeader(b.getHashPrev().getBytes())
            ) {
                log.debug("try to fetch orphans, head height {} hash {}", b.getHeight(), b.getHash());
                // remote: prune < b <= best
                GetBlocks getBlocks = new GetBlocks(
                        s.getPrunedHeight(), b.getHeight(), true,
                        syncConfig.getMaxBlocksTransfer()
                ).clip();

                ctx.response(SyncMessage.encode(SyncMessage.GET_BLOCKS, getBlocks));
            }
        }
        if (s.getBestBlockHeight() >= best.getHeight() && !s.getBestBlockHash().equals(best.getHash())) {
            GetBlocks getBlocks = new GetBlocks(
                    Math.max(s.getPrunedHeight(), best.getHeight()),
                    s.getBestBlockHeight(), false,
                    syncConfig.getMaxBlocksTransfer()
            ).clip();
            ctx.response(SyncMessage.encode(SyncMessage.GET_BLOCKS, getBlocks));
        }
    }

    private List<Block> getOrphansInternal() {
        List<Block> orphanHeads = new ArrayList<>();
        Set<HexBytes> orphans = new HashSet<>();
        Set<HexBytes> noOrphans = new HashSet<>();
        for (Block block : queue) {
            if (noOrphans.contains(block.getHashPrev())) {
                noOrphans.add(block.getHash());
                continue;
            }
            if (orphans.contains(block.getHashPrev())) {
                orphans.add(block.getHash());
                continue;
            }
            if (repository.containsHeader(block.getHashPrev().getBytes())) {
                noOrphans.add(block.getHash());
            } else {
                orphanHeads.add(block);
                orphans.add(block.getHash());
            }
        }
        return orphanHeads;
    }

    @SneakyThrows
    public List<Block> getOrphans() {
        if (!blockQueueLock.tryLock(syncConfig.getLockTimeout(), TimeUnit.SECONDS)) {
            throw new RuntimeException("busy...");
        }
        try {
            List<Block> ret = new ArrayList<>();
            Set<HexBytes> orphans = new HashSet<>();
            Set<HexBytes> noOrphans = new HashSet<>();
            for (Block block : queue) {
                if (noOrphans.contains(block.getHashPrev())) {
                    noOrphans.add(block.getHash());
                    continue;
                }
                if (orphans.contains(block.getHashPrev())) {
                    orphans.add(block.getHash());
                    ret.add(block);
                    continue;
                }
                if (repository.containsHeader(block.getHashPrev().getBytes())) {
                    noOrphans.add(block.getHash());
                } else {
                    ret.add(block);
                    orphans.add(block.getHash());
                }
            }
            return ret;
        } finally {
            blockQueueLock.unlock();
        }
    }

    @SneakyThrows
    public void tryWrite() {
        if (fastSyncing)
            return;
        Header best = repository.getBestHeader();
        Set<HexBytes> orphans = new HashSet<>();
        if (!blockQueueLock.tryLock(8 * syncConfig.getLockTimeout(), TimeUnit.SECONDS))
            return;
        Iterator<Block> it = queue.iterator();
        try {
            while (it.hasNext()) {
                Block b = it.next();
                if (Math.abs(best.getHeight() - b.getHeight()) > syncConfig.getMaxAccountsTransfer()
//                        || b.getHeight() <= repository.getPrunedHeight()
                ) {
                    it.remove();
                    continue;
                }
                if (repository.containsHeader(b.getHash().getBytes())) {
                    it.remove();
                    continue;
                }
                if (orphans.contains(b.getHashPrev())) {
                    orphans.add(b.getHash());
                    continue;
                }
                Optional<Block> o = repository.getBlock(b.getHashPrev().getBytes());
                if (!o.isPresent()) {
                    orphans.add(b.getHash());
                    continue;
                }
                ValidateResult res = engine.getValidator().validate(b, o.get());
                if (!res.isSuccess()) {
                    it.remove();
                    log.error(res.getReason());
                    continue;
                }
                it.remove();
                BlockValidateResult rs = ((BlockValidateResult) res);
                repository.writeBlock(b, rs.getInfos());
            }
        } finally {
            blockQueueLock.unlock();
        }
    }

    public void sendStatus() {
        if (fastSyncing) return;
        Header best = repository.getBestHeader();
        Block genesis = repository.getGenesis();
        Status status = new Status(
                best.getHeight(),
                best.getHash(),
                genesis.getHash(),
                0,
                null
//                repository.getPrunedHeight(),
//                repository.getPrunedHash()
        );
        broadcastToApproved(SyncMessage.encode(SyncMessage.STATUS, status));
    }


    @Override
    public void onStart(PeerServer server) {

    }

    @Override
    public void onNewPeer(Peer peer, PeerServer server) {

    }

    @Override
    public void onDisconnect(Peer peer, PeerServer server) {

    }

    @SneakyThrows
    public void close() {
        log.info("close sync manager");
        this.executorService.shutdown();
        this.executorService.awaitTermination(ApplicationConstants.MAX_SHUTDOWN_WAITING, TimeUnit.SECONDS);
    }
}
