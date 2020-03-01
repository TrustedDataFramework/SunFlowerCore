package org.tdf.sunflower.sync;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tdf.common.event.EventBus;
import org.tdf.common.store.CachedStore;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.ByteArrayMap;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.ApplicationConstants;
import org.tdf.sunflower.SyncConfig;
import org.tdf.sunflower.events.NewBlockMined;
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
    // when fastSyncing = true, the node
    private volatile boolean fastSyncing;
    private volatile Trie<HexBytes, Account> fastSyncTrie;
    private volatile Set<HexBytes> fastSyncAddresses;

    public SyncManager(
            PeerServer peerServer, ConsensusEngineFacade engine,
            SunflowerRepository repository,
            TransactionPool transactionPool, SyncConfig syncConfig,
            EventBus eventBus,
            AccountTrie accountTrie
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
    }

    private void clearFastSyncCache() {
        this.fastSyncBlock = null;
        this.fastSyncAddresses = null;
        this.fastSyncTrie = null;
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
                Status s = msg.getBodyAs(Status.class);
                this.onStatus(context, server, s);
                return;
            }
            case SyncMessage.GET_BLOCKS: {
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
                Block best = repository.getBestBlock();
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
                accountTrie.getTrie(root).traverse(e ->{
                    Account a = (Account) e.getValue();
                    addresses.add(a.getAddress());
                    return true;
                });
                context.response(SyncMessage.encode(SyncMessage.ADDRESSES, addresses));
                return;
            }
            case SyncMessage.ADDRESSES: {
                if (!fastSyncing || fastSyncAddresses != null) return;
                this.fastSyncAddresses = new HashSet<>(Arrays.asList(msg.getBodyAs(HexBytes[].class)));
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
                    }
                    if (fastSyncAddresses.isEmpty()) {
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
                } finally {
                    fastSyncAddressesLock.writeLock().unlock();
                }
            }
            case SyncMessage.BLOCKS: {
                Block[] blocks = msg.getBodyAs(Block[].class);
                if (fastSyncing) {
                    for (Block b : blocks) {
                        if (b.getHash().equals(fastSyncHash)) {
                            this.fastSyncBlock = b;
                            return;
                        }
                    }
                    return;
                }
                Block best = repository.getBestBlock();
                Arrays.sort(blocks, Block.FAT_COMPARATOR);
                blockQueueLock.lock();
                try {
                    for (Block block : blocks) {
                        if (block.getHeight() <= repository.getPrunedHeight())
                            continue;
                        if (Math.abs(block.getHeight() - best.getHeight()) > syncConfig.getMaxPendingBlocks())
                            break;
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


    private void onStatus(Context ctx, PeerServer server, Status s) {
        if (fastSyncing) {
            boolean fastSyncEnabled =
                    s.getPrunedHeight() > fastSyncHeight
                            || s.getPrunedHash().equals(fastSyncHash);
            if (!fastSyncEnabled && server.isFull()) {
                log.info("cannot fast sync by peer " + ctx.getRemote() + " block him");
                ctx.block();
                return;
            }
            if (fastSyncBlock == null) {
                ctx.response(
                        SyncMessage.encode(
                                SyncMessage.GET_BLOCKS,
                                new GetBlocks(fastSyncHeight, fastSyncHeight, false, syncConfig.getMaxBlocksTransfer())
                        ));
                log.info("fetch fast sync block at height {}", fastSyncHeight);
            }
            if (fastSyncBlock != null) {
                ctx.response(SyncMessage.encode(
                        SyncMessage.GET_ADDRESSES,
                        fastSyncBlock.getStateRoot()
                ));
                log.info("fetch account addresses");
            }
            if (fastSyncBlock != null && fastSyncAddresses != null) {
                fastSyncAddressesLock.readLock().lock();
                List<HexBytes> accountAddresses = new ArrayList<>();
                try {
                    int i = 0;
                    for (HexBytes address : fastSyncAddresses) {
                        if (i > syncConfig.getMaxAccountsTransfer()) break;
                        accountAddresses.add(address);
                        i++;
                    }
                } finally {
                    fastSyncAddressesLock.readLock().unlock();
                }
                log.info("try to fetch addresses at state root " + fastSyncBlock.getHeader());
                ctx.response(
                        SyncMessage.encode(
                                SyncMessage.GET_ACCOUNTS,
                                new GetAccounts(fastSyncBlock.getStateRoot(), accountAddresses)
                        )
                );
            }
            return;
        }
        Block b = null;
        Block best = repository.getBestBlock();
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
        ) {
            GetBlocks getBlocks = new GetBlocks(best.getHeight(), b.getHeight(), true, syncConfig.getMaxBlocksTransfer());
            ctx.response(SyncMessage.encode(SyncMessage.GET_BLOCKS, getBlocks));
        }
        if (s.getBestBlockHeight() >= best.getHeight() && !s.getBestBlockHash().equals(best.getHash())) {
            GetBlocks getBlocks = new GetBlocks(best.getHeight(), s.getBestBlockHeight(), false, syncConfig.getMaxBlocksTransfer());
            ctx.response(SyncMessage.encode(SyncMessage.GET_BLOCKS, getBlocks));
        }
    }

    public void tryWrite() {
        blockQueueLock.lock();
        try {
            while (true) {
                Block b = null;
                try {
                    b = queue.first();
                } catch (NoSuchElementException ignored) {
                }
                if (b == null) return;
                if (b.getHeight() <= repository.getPrunedHeight()) {
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
        Block best = repository.getBestBlock();
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
