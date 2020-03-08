package org.tdf.sunflower.sync;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.tdf.common.event.EventBus;
import org.tdf.common.store.CachedStore;
import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.ByteArrayMap;
import org.tdf.common.util.FastByteComparisons;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.ApplicationConstants;
import org.tdf.sunflower.SyncConfig;
import org.tdf.sunflower.crypto.CryptoContext;
import org.tdf.sunflower.events.NewBlockMined;
import org.tdf.sunflower.events.NewTransactionCollected;
import org.tdf.sunflower.facade.*;
import org.tdf.sunflower.net.Context;
import org.tdf.sunflower.net.Peer;
import org.tdf.sunflower.net.PeerServer;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.AccountTrie;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.types.ValidateResult;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.tdf.sunflower.Start.devAssert;

/**
 * sync manager for full-nodes
 */
@Component
@Slf4j(topic = "sync")
public class SyncManager implements PeerServerListener {
    private final PeerServer peerServer;
    private final ConsensusEngineFacade engine;
    private final SunflowerRepository repository;
    private final TransactionPool transactionPool;
    private final TreeSet<Block> queue = new TreeSet<>(Block.FAT_COMPARATOR);
    private final SyncConfig syncConfig;
    private final EventBus eventBus;
    private final StateTrie<HexBytes, Account> accountTrie;
    private final long fastSyncHeight;
    private final HexBytes fastSyncHash;
    private final Trie<byte[], byte[]> contractStorageTrie;
    private final Store<byte[], byte[]> contractCodeStore;
    private final Miner miner;

    private Limiters limiters;
    private volatile Block fastSyncBlock;

    private Cache<HexBytes, Boolean> receivedTransactions = CacheBuilder.newBuilder()
            .maximumSize(ApplicationConstants.P2P_TRANSACTION_CACHE_SIZE)
            .build();
    private Cache<HexBytes, Boolean> receivedProposals = CacheBuilder.newBuilder()
            .maximumSize(ApplicationConstants.P2P_PROPOSAL_CACHE_SIZE)
            .build();
    private Lock blockQueueLock = new ReentrantLock();

    // lock when another node ask for all addresses in the trie, avoid concurrent traverse
    private volatile boolean trieTraverseLock;

    private Lock fastSyncAddressesLock = new ReentrantLock();

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    // when fastSyncing = true, the node is in fast-syncing mode
    private volatile boolean fastSyncing;
    private volatile Trie<HexBytes, Account> fastSyncTrie;

    // not null when accounts transports
    private volatile Set<HexBytes> fastSyncAddresses;

    public SyncManager(
            PeerServer peerServer, ConsensusEngineFacade engine,
            SunflowerRepository repository,
            TransactionPool transactionPool, SyncConfig syncConfig,
            EventBus eventBus,
            AccountTrie accountTrie,
            @Qualifier("contractStorageTrie") Trie<byte[], byte[]> contractStorageTrie,
            @Qualifier("contractCodeStore") Store<byte[], byte[]> contractCodeStore,
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
        if (this.fastSyncing)
            this.miner.stop();
    }

    @PostConstruct
    public void init() {
        peerServer.addListeners(this);
        executorService.scheduleWithFixedDelay(
                this::tryWrite, 0,
                syncConfig.getBlockWriteRate(), TimeUnit.SECONDS);

        executorService.scheduleWithFixedDelay(
                this::sendStatus, 0,
                syncConfig.getHeartRate(), TimeUnit.SECONDS
        );
        eventBus.subscribe(NewBlockMined.class, (e) -> propose(e.getBlock()));
        eventBus.subscribe(NewTransactionCollected.class, (e) -> {
            if (receivedTransactions.asMap().containsKey(e.getTransaction().getHash())) return;
            receivedTransactions.asMap().put(e.getTransaction().getHash(), true);
            peerServer.broadcast(SyncMessage.encode(SyncMessage.TRANSACTION, e.getTransaction()));
        });
    }

    private void clearFastSyncCache() {
        this.fastSyncBlock = null;
        this.fastSyncTrie = null;
        this.fastSyncAddresses = null;
    }

    @Override
    @SneakyThrows
    public void onMessage(Context context, PeerServer server) {
        Optional<SyncMessage> o = SyncMessage.decode(context.getMessage());
        if (!o.isPresent()) return;
        SyncMessage msg = o.get();
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
                Transaction tx = msg.getBodyAs(Transaction.class);
                if (receivedTransactions.asMap().containsKey(tx.getHash())) return;
                receivedTransactions.put(tx.getHash(), true);
                context.relay();
                transactionPool.collect(tx);
                return;
            }
            case SyncMessage.PROPOSAL: {
                if (fastSyncing) {
                    context.relay();
                    return;
                }
                Header best = repository.getBestHeader();
                Block proposal = msg.getBodyAs(Block.class);
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
                if(!blockQueueLock.tryLock(syncConfig.getLockTimeout(), TimeUnit.SECONDS))
                    return;
                try {
                    queue.add(proposal);
                } finally {
                    blockQueueLock.unlock();
                }
                return;
            }
            case SyncMessage.GET_ACCOUNTS: {
                if (fastSyncing || this.trieTraverseLock) return;
                this.trieTraverseLock = true;
                CompletableFuture.runAsync(() -> {
                    try {
                        GetAccounts getAccounts = msg.getBodyAs(GetAccounts.class);
                        Trie<HexBytes, Account> trie = accountTrie.getTrie(getAccounts.getStateRoot().getBytes());
                        int[] total = new int[1];
                        List<SyncAccount> accounts = new ArrayList<>();
                        trie.traverse((e) -> {
                            Account a = e.getValue();
                            List<byte[]> kv = new ArrayList<>();
                            if (a.getStorageRoot() != null && a.getStorageRoot().length != 0) {
                                contractStorageTrie.revert(a.getStorageRoot())
                                        .forEach((k, v) -> {
                                            kv.add(k);
                                            kv.add(v);
                                });
                            }
                            accounts.add(new SyncAccount(
                                    a,
                                    (a.getContractHash() == null || a.getContractHash().length == 0) ?
                                            null :
                                            contractCodeStore.get(a.getContractHash()).get(),
                                    kv
                            ));
                            total[0]++;
                            if (accounts.size() % Math.min(syncConfig.getMaxAccountsTransfer(), getAccounts.getMaxAccounts()) == 0) {
                                context.response(
                                        SyncMessage.encode(SyncMessage.ACCOUNTS, new Accounts(0, accounts, false))
                                );
                                log.info("{} accounts traversed", total[0]);
                                accounts.clear();
                            }
                            return true;
                        });
                        context.response(SyncMessage.encode(SyncMessage.ACCOUNTS, new Accounts(total[0], accounts, true)));
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        this.trieTraverseLock = false;
                    }
                });
                return;
            }
            case SyncMessage.ACCOUNTS: {
                if (!fastSyncing) return;
                if (fastSyncAddresses == null) {
                    fastSyncAddresses = new HashSet<>();
                }
                if (fastSyncTrie == null) {
                    fastSyncTrie = accountTrie.getTrie().revert(
                            accountTrie.getTrie().getNullHash(),
                            new CachedStore<>(accountTrie.getTrieStore(), ByteArrayMap::new)
                    );
                }
                this.fastSyncAddressesLock.lock();
                try {
                    Accounts accounts = msg.getBodyAs(Accounts.class);
                    for (SyncAccount sa : accounts.getAccounts()) {
                        Account a = sa.getAccount();
                        if(this.fastSyncAddresses.contains(a.getAddress()))
                            continue;
                        // validate contract code
                        if (a.getContractHash() != null && a.getContractHash().length != 0) {
                            byte[] key = CryptoContext.digest(sa.getContractCode());
                            if (!FastByteComparisons.equal(key, a.getContractHash())) {
                                log.error("contract hash not match");
                                continue;
                            }
                            contractCodeStore.put(key, sa.getContractCode());
                        }

                        // validate storage root
                        if(a.getStorageRoot() != null && a.getStorageRoot().length != 0){
                            Trie<byte[], byte[]> empty = contractStorageTrie.revert();
                            for (int i = 0; i < sa.getContractStorage().size() / 2; i += 1) {
                                byte[] k = sa.getContractStorage().get(2 * i);
                                byte[] v = sa.getContractStorage().get(2 * i + 1);
                                empty.put(k, v);
                            }

                            byte[] root = empty.commit();
                            if (!FastByteComparisons.equal(root, a.getStorageRoot())) {
                                log.error("storage root not match");
                                continue;
                            }
                        }

                        fastSyncAddresses.add(a.getAddress());
                        fastSyncTrie.put(a.getAddress(), a);
                    }
                    log.info("synced accounts = " + fastSyncAddresses.size());
                    if (!accounts.isTraversed() || fastSyncAddresses.size() != accounts.getTotal()) {
                        return;
                    }
                    HexBytes stateRoot = HexBytes.fromBytes(fastSyncTrie.commit());
                    if (!fastSyncBlock.getStateRoot().equals(stateRoot)) {
                        clearFastSyncCache();
                        log.error("fast sync failed, state root not match, malicious node may exists in network!!!");
                        return;
                    }
                    finishFastSync();
                } finally {
                    this.fastSyncAddressesLock.unlock();
                }
                return;
            }

            case SyncMessage.BLOCKS: {
                Block[] blocks = msg.getBodyAs(Block[].class);
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
                Arrays.sort(blocks, Block.FAT_COMPARATOR);
                if(!blockQueueLock.tryLock(syncConfig.getLockTimeout(), TimeUnit.SECONDS))
                    return;
                try {
                    for (Block block : blocks) {
                        if (block.getHeight() <= repository.getPrunedHeight())
                            continue;
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
                return;
            }
        }
    }

    private void finishFastSync() {
        this.fastSyncing = false;
        fastSyncTrie.flush();
        repository.writeBlock(fastSyncBlock);
        repository.prune(fastSyncBlock.getHash().getBytes());
        devAssert(
                repository.getPrunedHash().equals(fastSyncBlock.getHash()), "prune failed after fast sync");
        log.info("fast sync success to height {} hash {}", fastSyncHeight, fastSyncBlock.getHash());
        this.miner.start();
        clearFastSyncCache();
    }

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
        Block b = null;
        Header best = repository.getBestHeader();
        if(!blockQueueLock.tryLock(syncConfig.getLockTimeout(), TimeUnit.SECONDS))
            return;
        try {
            b = queue.first();
        } catch (NoSuchElementException ignored) {

        } finally {
            blockQueueLock.unlock();
        }
        if (b != null
                && s.getBestBlockHeight() >= b.getHeight()
                && b.getHeight() > s.getPrunedHeight()
                && !repository.containsHeader(b.getHashPrev().getBytes())
        ) {
            // remote: prune < b <= best
            GetBlocks getBlocks = new GetBlocks(
                    s.getPrunedHeight(), b.getHeight(), true,
                    syncConfig.getMaxBlocksTransfer()
            ).clip();

            ctx.response(SyncMessage.encode(SyncMessage.GET_BLOCKS, getBlocks));
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

    @SneakyThrows
    public void tryWrite() {
        if (fastSyncing)
            return;
        Header best = repository.getBestHeader();
        if(!blockQueueLock.tryLock(syncConfig.getLockTimeout(), TimeUnit.SECONDS))
            return;
        try {
            while (true) {
                Block b = null;
                try {
                    b = queue.first();
                } catch (NoSuchElementException ignored) {
                }
                if (b == null) return;
                if (Math.abs(best.getHeight() - b.getHeight()) > syncConfig.getMaxAccountsTransfer()
                        || b.getHeight() <= repository.getPrunedHeight()
                ) {
                    queue.remove(b);
                    continue;
                }
                if (repository.containsHeader(b.getHash().getBytes())) {
                    queue.remove(b);
                    continue;
                }
                Optional<Block> o = repository.getBlock(b.getHashPrev().getBytes());
                if (!o.isPresent()) {
                    return;
                }
                ValidateResult res = engine.getValidator().validate(b, o.get());
                if (!res.isSuccess()) {
                    queue.remove(b);
                    log.error(res.getReason());
                    continue;
                }
                queue.remove(b);
                repository.writeBlock(b);
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
                repository.getPrunedHeight(),
                repository.getPrunedHash()
        );
        peerServer.broadcast(SyncMessage.encode(SyncMessage.STATUS, status));
    }


    public void propose(Block b) {
        peerServer.broadcast(SyncMessage.encode(SyncMessage.PROPOSAL, b));
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
}
