package org.tdf.sunflower.pool;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tdf.common.event.EventBus;
import org.tdf.common.store.Store;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.ByteUtil;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.ApplicationConstants;
import org.tdf.sunflower.TransactionPoolConfig;
import org.tdf.sunflower.events.NewBestBlock;
import org.tdf.sunflower.events.NewTransactionsCollected;
import org.tdf.sunflower.facade.ConsensusEngine;
import org.tdf.sunflower.facade.PendingTransactionValidator;
import org.tdf.sunflower.facade.SunflowerRepository;
import org.tdf.sunflower.facade.TransactionPool;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.*;
import org.tdf.sunflower.vm.Backend;
import org.tdf.sunflower.vm.CallData;
import org.tdf.sunflower.vm.VMExecutor;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j(topic = "txPool")
@Component
public class TransactionPoolImpl implements TransactionPool {
    private final EventBus eventBus;

    // waited
    private final TreeSet<TransactionInfo> cache;
    // hash -> info
    private final Map<HexBytes, TransactionInfo> mCache;

    private final ScheduledExecutorService poolExecutor;
    private final TransactionPoolConfig config;
    private final SunflowerRepository repository;


    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    // dropped transactions
    private final Cache<HexBytes, Transaction> dropped;


    private PendingTransactionValidator validator;

    // pending transaction
    private Header parentHeader;
    private StateTrie<HexBytes, Account> trie;
    private List<Transaction> pending;
    private List<TransactionReceipt> pendingReceipts;
    private HexBytes currentRoot;

    public void reset(Header best) {
        this.cacheLock.writeLock().lock();
        try {
            this.parentHeader = best;
            this.currentRoot = best.getHash();
            this.pending = new ArrayList<>();
            this.pendingReceipts = new ArrayList<>();
        } finally {
            this.cacheLock.writeLock().unlock();
        }
    }

    public TransactionPoolImpl(
        EventBus eventBus,
        TransactionPoolConfig config,
        SunflowerRepository repository
    ) {
        this.eventBus = eventBus;
        eventBus.subscribe(NewBestBlock.class, this::onNewBestBlock);
        cache = new TreeSet<>();
        this.config = config;


        poolExecutor = Executors.newSingleThreadScheduledExecutor();
        poolExecutor.scheduleWithFixedDelay(this::clear, 0, config.getExpiredIn(), TimeUnit.SECONDS);


        dropped = CacheBuilder.newBuilder()
            .expireAfterWrite(config.getExpiredIn(), TimeUnit.SECONDS)
            .build();


        this.repository = repository;
        this.mCache = new HashMap<>();

    }

    public void setEngine(ConsensusEngine engine) {
        this.validator = engine.getValidator();
        this.trie = engine.getAccountTrie();
        Header best = repository.getBestHeader();
        this.parentHeader = best;
        this.currentRoot = best.getStateRoot();
        this.pending = new ArrayList<>();
        this.pendingReceipts = new ArrayList<>();
    }

    private void clearPending() {
        this.pending = new ArrayList<>();
        this.pendingReceipts = new ArrayList<>();
        this.currentRoot = null;
        this.parentHeader = null;
    }

    @SneakyThrows
    private void clear() {
        long now = System.currentTimeMillis();
        if (!this.cacheLock.writeLock().tryLock(config.getLockTimeout(), TimeUnit.SECONDS)) {
            return;
        }
        try {
            Predicate<TransactionInfo> lambda =
                info -> {
                    boolean remove = (now - info.receivedAt) / 1000 > config.getExpiredIn();
                    if (remove) {
                        dropped.put(info.tx.getHashHex(), info.tx);
                    }
                    return remove;
                };
            cache.removeIf(lambda);
            mCache.values().removeIf(lambda);
        } finally {
            this.cacheLock.writeLock().unlock();
        }
    }

    @Override
    @SneakyThrows
    public Map<HexBytes, String> collect(Collection<? extends Transaction> transactions) {
        Map<HexBytes, String> errors = new HashMap<>();
        this.cacheLock.writeLock().lock();
        try {
            List<Transaction> newCollected = new ArrayList<>(transactions.size());
            for (Transaction transaction : transactions) {
                if (transaction.getGasPriceAsU256().compareTo(Uint256.of(ApplicationConstants.VM_GAS_PRICE)) < 0)
                    throw new RuntimeException("transaction pool: gas price of tx less than vm gas price " + ApplicationConstants.VM_GAS_PRICE);

                TransactionInfo info = new TransactionInfo(System.currentTimeMillis(), transaction);
                if (cache.contains(info) || dropped.asMap().containsKey(transaction.getHashHex()))
                    continue;
                try {
                    transaction.verify();
                } catch (Exception e) {
                    log.error(e.getMessage());
                    continue;
                }
                ValidateResult res = validator.validate(parentHeader, transaction);
                if (res.isSuccess()) {
                    cache.add(info);
                    mCache.put(info.tx.getHashHex(), info);
                    newCollected.add(transaction);
                } else {
                    log.error(res.getReason());
                    errors.put(transaction.getHashHex(), res.getReason());
                }
            }
            if (!errors.isEmpty())
                newCollected = Collections.emptyList();

            if (!newCollected.isEmpty())
                eventBus.publish(new NewTransactionsCollected(newCollected));

            this.execute(errors);
        } finally {
            this.cacheLock.writeLock().unlock();
        }
        return errors;
    }

    @SneakyThrows
    private void execute(Map<HexBytes, String> errors) {
        Iterator<TransactionInfo> it = cache.iterator();
        if(this.parentHeader == null)
            return;

        Backend current = trie.createBackend(parentHeader, currentRoot, null, false);

        while (it.hasNext()) {
            Transaction t = it.next().tx;
            long prevNonce = current.getNonce(t.getSenderHex());

            if (t.getNonceAsLong() < prevNonce) {
                it.remove();
                errors.put(t.getHashHex(), "nonce is too small");
                mCache.remove(t.getHashHex());
                dropped.put(t.getHashHex(), t);
                continue;
            }

            if (t.getNonceAsLong() != prevNonce) {
                continue;
            }

            // try to execute

            try {
                Backend child = current.createChild();
                CallData callData = CallData.fromTransaction(t, false);
                VMExecutor vmExecutor = new VMExecutor(child, callData);
                VMResult res = vmExecutor.execute();

                // execute successfully
                long currentGas =
                    pendingReceipts.size() > 0 ?
                        ByteUtil.byteArrayToLong(pendingReceipts.get(pendingReceipts.size() - 1).getCumulativeGas()) : 0;

                TransactionReceipt receipt = new TransactionReceipt(
                    current.getTrieRoot().getBytes(),
                    ByteUtil.longToBytesNoLeadZeroes(currentGas + res.getGasUsed()),
                    new Bloom(),
                    Collections.emptyList()
                );

                receipt.setGasUsed(res.getGasUsed());
                receipt.setExecutionResult(res.getExecutionResult());
                receipt.setTransaction(t);
                pending.add(t);
                pendingReceipts.add(receipt);
                this.currentRoot = child.merge();
            } catch (Exception e) {
                errors.put(t.getHashHex(), e.getMessage());
            } finally {
                it.remove();
                mCache.remove(t.getHashHex());
            }
        }

    }

    @SneakyThrows
    public PendingData pop(Header parentHeader) {
        this.cacheLock.writeLock().lock();
        try {
            if(this.parentHeader != null && !parentHeader.getHash().equals(this.parentHeader.getHash())) {
                this.clearPending();
                log.warn("parent header is not equal, drop pending");
            }
            boolean afterClean = this.parentHeader == null;
            PendingData r =
                new PendingData(
                    afterClean ? new ArrayList<>() : this.pending,
                    afterClean ? new ArrayList<>() : this.pendingReceipts,
                    afterClean ? parentHeader.getStateRoot() : this.currentRoot);
            return r;
        } finally {
            this.cacheLock.writeLock().unlock();
        }
    }

    @SneakyThrows
    public void onNewBestBlock(NewBestBlock event) {
        this.cacheLock.writeLock().tryLock();
        try {
            if(event.getBlock().getStateRoot().equals(this.parentHeader.getStateRoot()))
                return;

            this.parentHeader = event.getBlock().getHeader();
            this.currentRoot = event.getBlock().getStateRoot();
            this.pending = new ArrayList<>();
            this.pendingReceipts = new ArrayList<>();
        } finally {
            this.cacheLock.writeLock().unlock();
        }
    }


    @AllArgsConstructor
    static class TransactionInfo implements Comparable<TransactionInfo> {
        private final long receivedAt;
        private final Transaction tx;

        @Override
        public int compareTo(TransactionInfo o) {
            int cmp = tx.getSenderHex().compareTo(o.tx.getSenderHex());
            if (cmp != 0) return cmp;
            cmp = Long.compare(tx.getNonceAsLong(), o.tx.getNonceAsLong());
            if (cmp != 0) return cmp;
            cmp = -tx.getGasPriceAsU256().compareTo(o.tx.getGasPriceAsU256());
            if (cmp != 0) return cmp;
            return tx.getHashHex().compareTo(o.tx.getHashHex());
        }
    }
}
