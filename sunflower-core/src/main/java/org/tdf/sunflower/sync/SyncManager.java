package org.tdf.sunflower.sync;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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
import org.tdf.sunflower.facade.ConsensusEngineFacade;
import org.tdf.sunflower.facade.PeerServerListener;
import org.tdf.sunflower.facade.SunflowerRepository;
import org.tdf.sunflower.facade.TransactionPool;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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

    private Limiters limiters;
    private volatile Block fastSyncBlock;

    private Cache<HexBytes, Boolean> receivedTransactions = CacheBuilder.newBuilder()
            .maximumSize(ApplicationConstants.P2P_TRANSACTION_CACHE_SIZE)
            .build();
    private Cache<HexBytes, Boolean> receivedProposals = CacheBuilder.newBuilder()
            .maximumSize(ApplicationConstants.P2P_PROPOSAL_CACHE_SIZE)
            .build();
    private Lock blockQueueLock = new ReentrantLock();

    private ReadWriteLock fastSyncAddressesLock = new ReentrantReadWriteLock();

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    // when fastSyncing = true, the node is in fast-syncing mode
    private volatile boolean fastSyncing;
    private volatile Trie<HexBytes, Account> fastSyncTrie;
    private volatile Set<HexBytes> fastSyncAddresses;
    private volatile Set<HexBytes> fastSyncContracts;
    private volatile TreeSet<HexBytes> fastSyncStorageRoots;

    public SyncManager(
            PeerServer peerServer, ConsensusEngineFacade engine,
            SunflowerRepository repository,
            TransactionPool transactionPool, SyncConfig syncConfig,
            EventBus eventBus,
            AccountTrie accountTrie,
            @Qualifier("contractStorageTrie") Trie<byte[], byte[]> contractStorageTrie,
            @Qualifier("contractCodeStore") Store<byte[], byte[]> contractCodeStore
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
        eventBus.subscribe(NewBlockMined.class, (e) -> {
            propose(e.getBlock());
        });
        eventBus.subscribe(NewTransactionCollected.class, (e) -> {
            if (receivedTransactions.asMap().containsKey(e.getTransaction().getHash())) return;
            receivedTransactions.asMap().put(e.getTransaction().getHash(), true);
            peerServer.broadcast(SyncMessage.encode(SyncMessage.TRANSACTION, e.getTransaction()));
        });
    }

    private void clearFastSyncCache() {
        this.fastSyncBlock = null;
        this.fastSyncAddresses = null;
        this.fastSyncTrie = null;
        this.fastSyncContracts = null;
        this.fastSyncStorageRoots = null;
    }

    @Override
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
                blockQueueLock.lock();
                try {
                    queue.add(proposal);
                } finally {
                    blockQueueLock.unlock();
                }
                return;
            }
            case SyncMessage.GET_ADDRESSES: {
                if (fastSyncing) return;
                byte[] root = msg.getBodyAs(byte[].class);
                Set<HexBytes> addresses = new HashSet<>();
                accountTrie.getTrie(root).traverse(e -> {
                    Account a = e.getValue();
                    addresses.add(a.getAddress());
                    return true;
                });
                context.response(SyncMessage.encode(SyncMessage.ADDRESSES, addresses));
                return;
            }
            case SyncMessage.ADDRESSES: {
                if (!fastSyncing || fastSyncAddresses != null) return;
                this.fastSyncAddresses = new HashSet<>(Arrays.asList(msg.getBodyAs(HexBytes[].class)));
                this.fastSyncContracts = new HashSet<>();
                this.fastSyncStorageRoots = new TreeSet<>();
                return;
            }
            case SyncMessage.GET_ACCOUNTS: {
                if (fastSyncing) return;
                GetAccounts getAccounts = msg.getBodyAs(GetAccounts.class);
                List<Account> accounts = new ArrayList<>();
                Trie<HexBytes, Account> trie = accountTrie.getTrie(getAccounts.getStateRoot().getBytes());
                int i = 0;
                for (HexBytes address : getAccounts.getAddresses()) {
                    if (i > syncConfig.getMaxAccountsTransfer())
                        break;
                    accounts.add(trie.get(address).get());
                    i++;
                }
                context.response(SyncMessage.encode(SyncMessage.ACCOUNTS, accounts));
                return;
            }
            case SyncMessage.GET_CONTRACTS: {
                if(fastSyncing) return;
                byte[][] contractHashes = msg.getBodyAs(byte[][].class);
                List<byte[]> ret = new ArrayList<>(contractHashes.length);
                for (byte[] hash : contractHashes) {
                    contractCodeStore.get(hash).ifPresent(ret::add);
                }
                context.response(SyncMessage.encode(SyncMessage.CONTRACTS, ret));
                return;
            }
            case SyncMessage.CONTRACTS: {
                if (!fastSyncing || fastSyncContracts == null) return;
                byte[][] contracts = msg.getBodyAs(byte[][].class);
                fastSyncAddressesLock.writeLock().lock();
                try {
                    for (byte[] contract : contracts) {
                        byte[] key = CryptoContext.digest(contract);
                        if (!fastSyncContracts.contains(HexBytes.fromBytes(key)))
                            continue;
                        fastSyncContracts.remove(HexBytes.fromBytes(key));
                        contractCodeStore.put(key, contract);
                    }
                } finally {
                    fastSyncAddressesLock.writeLock().unlock();
                }
                tryFinishFastSync();
                return;
            }
            case SyncMessage.CONTRACT_STORAGE: {
                if (!fastSyncing || fastSyncContracts == null) return;
                byte[][] kv = msg.getBodyAs(byte[][].class);
                fastSyncAddressesLock.writeLock().lock();
                Trie<byte[], byte[]> empty = contractStorageTrie.revert();
                try {
                    for (int i = 0; i < kv.length/2; i += 1) {
                        byte[] k = kv[2 * i];
                        byte[] v = kv[2 * i + 1];
                        empty.put(k, v);
                    }
                    byte[] root = empty.commit();
                    fastSyncStorageRoots.remove(HexBytes.fromBytes(root));
                } finally {
                    fastSyncAddressesLock.writeLock().unlock();
                }
                tryFinishFastSync();
                return;
            }
            case SyncMessage.GET_CONTRACT_STORAGE: {
                if(fastSyncing)return;
                byte[] root = msg.getBodyAs(byte[].class);
                List<byte[]> ret = new ArrayList<>();
                this.contractStorageTrie.revert(root)
                        .forEach((k, v) -> {
                            ret.add(k);
                            ret.add(v);
                        });
                context.response(SyncMessage.encode(SyncMessage.CONTRACT_STORAGE, ret));
                return;
            }
            case SyncMessage.ACCOUNTS: {
                if (!fastSyncing) return;
                if (fastSyncTrie == null) {
                    fastSyncTrie = accountTrie.getTrie().revert(
                            accountTrie.getTrie().getNullHash(),
                            new CachedStore<>(accountTrie.getTrieStore(), ByteArrayMap::new)
                    );
                }
                fastSyncAddressesLock.writeLock().lock();
                try {
                    for (Account a : msg.getBodyAs(Account[].class)) {
                        fastSyncTrie.put(a.getAddress(), a);
                        fastSyncAddresses.remove(a.getAddress());
                        if (a.containsContract() && !contractCodeStore.containsKey(a.getAddress().getBytes())) {
                            fastSyncContracts.add(HexBytes.fromBytes(a.getContractHash()));
                        }
                        if (
                                a.getStorageRoot() != null
                                        && a.getStorageRoot().length != 0
                                        && !FastByteComparisons.equal(contractStorageTrie.getNullHash(), a.getStorageRoot())
                                        && !contractStorageTrie.getStore().containsKey(a.getStorageRoot())
                        ) {
                            fastSyncStorageRoots.add(HexBytes.fromBytes(a.getStorageRoot()));
                        }
                    }
                    tryFinishFastSync();
                } finally {
                    fastSyncAddressesLock.writeLock().unlock();
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
                blockQueueLock.lock();
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

    private void tryFinishFastSync(){
        if (fastSyncAddresses.isEmpty() && fastSyncContracts.isEmpty() && fastSyncStorageRoots.isEmpty()) {
            HexBytes stateRoot = HexBytes.fromBytes(fastSyncTrie.commit());
            if (fastSyncBlock.getStateRoot().equals(stateRoot)) {
                this.fastSyncing = false;
                fastSyncTrie.flush();
                repository.writeBlock(fastSyncBlock);
                repository.prune(fastSyncBlock.getHash().getBytes());
                devAssert(
                        repository.getPrunedHash().equals(fastSyncBlock.getHash()), "prune failed after fast sync");
                log.info("fast sync success to height {} hash {}", fastSyncHeight, fastSyncBlock.getHash());
                clearFastSyncCache();
                return;
            } else {
                clearFastSyncCache();
                log.error("fast sync failed, state root not match, malicious node may exists in network!!!");
            }
        }
    }

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
                        SyncMessage.GET_ADDRESSES,
                        fastSyncBlock.getStateRoot()
                ));
                log.info("fetch account addresses");
            }
            if (
                    fastSyncBlock != null
                            && fastSyncAddresses != null
                            && fastSyncContracts != null
                            && fastSyncStorageRoots != null
            ) {
                fastSyncAddressesLock.readLock().lock();
                List<HexBytes> accountAddresses = new ArrayList<>();
                List<HexBytes> contractAddresses = new ArrayList<>();
                HexBytes root = null;
                try {
                    int i = 0;
                    for (HexBytes address : fastSyncAddresses) {
                        if (i > syncConfig.getMaxAccountsTransfer()) break;
                        accountAddresses.add(address);
                        i++;
                    }
                    i = 0;
                    for (HexBytes address : fastSyncContracts) {
                        if (i > syncConfig.getMaxAccountsTransfer()) break;
                        contractAddresses.add(address);
                        i++;
                    }
                    root = fastSyncStorageRoots.isEmpty() ? null : fastSyncStorageRoots.first();
                } finally {
                    fastSyncAddressesLock.readLock().unlock();
                }
                log.info("try to fetch addresses at state root " + fastSyncBlock.getHeader());
                if(!accountAddresses.isEmpty()){
                    ctx.response(
                            SyncMessage.encode(
                                    SyncMessage.GET_ACCOUNTS,
                                    new GetAccounts(fastSyncBlock.getStateRoot(), accountAddresses)
                            )
                    );
                }
                if(!contractAddresses.isEmpty()){
                    ctx.response(
                            SyncMessage.encode(
                                    SyncMessage.GET_CONTRACTS,
                                    contractAddresses
                            )
                    );
                }
                if(root != null){
                    ctx.response(
                            SyncMessage.encode(
                                    SyncMessage.GET_CONTRACT_STORAGE,
                                    root
                            )
                    );
                }
            }
            return;
        }
        Block b = null;
        Header best = repository.getBestHeader();
        blockQueueLock.lock();
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

    public void tryWrite() {
        Header best = repository.getBestHeader();
        blockQueueLock.lock();
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
