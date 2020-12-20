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
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.ApplicationConstants;
import org.tdf.sunflower.TransactionPoolConfig;
import org.tdf.sunflower.controller.WebSocket;
import org.tdf.sunflower.events.*;
import org.tdf.sunflower.facade.ConsensusEngine;
import org.tdf.sunflower.facade.PendingTransactionValidator;
import org.tdf.sunflower.facade.TransactionPool;
import org.tdf.sunflower.facade.TransactionRepository;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.types.PageSize;
import org.tdf.sunflower.types.PagedView;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.types.ValidateResult;

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

    private final TreeSet<TransactionInfo> cache;

    private final Map<HexBytes, TransactionInfo> mCache;
    private final ScheduledExecutorService poolExecutor;
    private final TransactionPoolConfig config;
    private final TransactionRepository transactionRepository;
    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    // dropped transactions
    private final Cache<HexBytes, Transaction> dropped;
    private PendingTransactionValidator validator;

    public TransactionPoolImpl(
            EventBus eventBus,
            TransactionPoolConfig config,
            TransactionRepository repository
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
        this.transactionRepository = repository;
        this.mCache = new HashMap<>();

        this.eventBus.subscribe(TransactionIncluded.class, (e) -> {
            WebSocket.broadCastIncluded(e.getTxHash(), e.getBlock().getHeight(), e.getBlock().getHash(), e.getGasUsed(), e.getReturns(), e.getEvents());
        });

        this.eventBus.subscribe(TransactionFailed.class, (e) -> {
            WebSocket.broadcastDrop(e.getTxHash(), e.getReason());
        });

        this.eventBus.subscribe(TransactionConfirmed.class, (e) -> {
            WebSocket.broadcastPendingOrConfirm(e.getTxHash(), Transaction.Status.CONFIRMED);
        });
    }

    public void setEngine(ConsensusEngine engine) {
        this.validator = engine.getValidator();
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
                        if (remove && !transactionRepository.containsTransaction(info.tx.getHash().getBytes())) {
                            WebSocket.broadcastDrop(info.tx.getHash(), "invalid nonce");
                            dropped.put(info.tx.getHash(), info.tx);
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
    public List<String> collect(Collection<? extends Transaction> transactions) {
        List<String> errors = new ArrayList<>();
        this.cacheLock.writeLock().lock();
        try {
            List<Transaction> newCollected = new ArrayList<>(transactions.size());
            for (Transaction transaction : transactions) {
                if (transaction.getGasPrice().compareTo(Uint256.of(ApplicationConstants.VM_GAS_PRICE)) < 0)
                    throw new RuntimeException("transaction pool: gas price of tx less than vm gas price " + ApplicationConstants.VM_GAS_PRICE);
                if (transaction.getAmount() == null)
                    transaction.setAmount(Uint256.ZERO);
                if (transaction.getGasPrice() == null)
                    transaction.setGasPrice(Uint256.ZERO);

                TransactionInfo info = new TransactionInfo(System.currentTimeMillis(), transaction);
                if (cache.contains(info) || dropped.asMap().containsKey(transaction.getHash()))
                    continue;
                ValidateResult res = transaction.basicValidate();
                if (!res.isSuccess()) {
                    log.error(res.getReason());
                    errors.add(res.getReason());
                    WebSocket.broadcastDrop(transaction.getHash(), res.getReason());
                    continue;
                }
                res = validator.validate(transaction);
                if (res.isSuccess()) {
                    cache.add(info);
                    mCache.put(info.tx.getHash(), info);
                    newCollected.add(transaction);
                } else {
                    WebSocket.broadcastDrop(transaction.getHash(), res.getReason());
                    log.error(res.getReason());
                    errors.add(res.getReason());
                }
            }
            if (!errors.isEmpty())
                newCollected = Collections.emptyList();

            for (Transaction tx : newCollected) {
                WebSocket.broadcastPendingOrConfirm(tx.getHash(), Transaction.Status.PENDING);
            }

            if (!newCollected.isEmpty())
                eventBus.publish(new NewTransactionsCollected(newCollected));
        } finally {
            this.cacheLock.writeLock().unlock();
        }
        return errors;
    }

    @SneakyThrows
    public List<Transaction> popPackable(Store<HexBytes, Account> accountStore, int limit) {
        this.cacheLock.writeLock().lock();
        try {
            Map<HexBytes, Long> nonceMap = new HashMap<>();

            Iterator<TransactionInfo> it = cache.iterator();
            List<Transaction> ret = new ArrayList<>();
            int count = 0;
            while (count < ((limit < 0) ? Long.MAX_VALUE : limit) && it.hasNext()) {
                Transaction t = it.next().tx;
                long prevNonce =
                        nonceMap.containsKey(t.getFromAddress()) ?
                                nonceMap.get(t.getFromAddress()) :
                                accountStore.get(t.getFromAddress())
                                        .map(Account::getNonce)
                                        .orElse(0L);
                if (t.getNonce() <= prevNonce) {
                    it.remove();
                    mCache.remove(t.getHash());
                    if (!transactionRepository.containsTransaction(t.getHash().getBytes()))
                        dropped.put(t.getHash(), t);
                    WebSocket.broadcastDrop(t.getHash(), "invalid nonce");
                    continue;
                }
                if (t.getNonce() != prevNonce + 1) {
                    continue;
                }
                nonceMap.put(t.getFromAddress(), t.getNonce());
                ret.add(t);
                it.remove();
                count++;
            }
            return ret;
        } finally {
            this.cacheLock.writeLock().unlock();
        }
    }

    @Override
    public int size() {
        return cache.size();
    }

    @Override
    @SneakyThrows
    public PagedView<Transaction> get(PageSize pageSize) {
        if (!this.cacheLock.readLock().tryLock(config.getLockTimeout(), TimeUnit.SECONDS)) {
            throw new RuntimeException("busying...");
        }
        try {
            List<Transaction> ret = cache.stream()
                    .skip(pageSize.getPage() * pageSize.getSize())
                    .limit(pageSize.getSize())
                    .map(info -> info.tx)
                    .collect(Collectors.toList());
            return new PagedView<>(pageSize.getPage(), pageSize.getSize(), cache.size(), ret);
        } finally {
            this.cacheLock.readLock().unlock();
        }
    }

    @Override
    public void setValidator(PendingTransactionValidator validator) {
        this.validator = validator;
    }

    @Override
    public void drop(Transaction transaction) {
        dropped.asMap().put(transaction.getHash(), transaction);
    }

    @SneakyThrows
    public void onNewBestBlock(NewBestBlock event) {
        if (!this.cacheLock.writeLock().tryLock(config.getLockTimeout(), TimeUnit.SECONDS)) {
            return;
        }
        try {
            event.getBlock().getBody().forEach(t ->
                    {
                        cache.remove(new TransactionInfo(System.currentTimeMillis(), t));
                        mCache.remove(t.getHash());
                    }
            );

        } finally {
            this.cacheLock.writeLock().unlock();
        }
    }

    @Override
    public PagedView<Transaction> getDropped(PageSize pageSize) {
        List<Transaction> ret = dropped.asMap().values()
                .stream()
                .skip(pageSize.getPage() * pageSize.getSize())
                .limit(pageSize.getSize())
                .collect(Collectors.toList());
        return new PagedView<>(pageSize.getPage(), pageSize.getSize(), dropped.asMap().size(), ret);
    }

    @Override
    public Optional<Transaction> get(HexBytes hash) {
        this.cacheLock.readLock().lock();
        try {
            return Optional.ofNullable(mCache.get(hash)).map(x -> x.tx);
        } finally {
            this.cacheLock.readLock().unlock();
        }
    }

    @AllArgsConstructor
    static class TransactionInfo implements Comparable<TransactionInfo> {
        private final long receivedAt;
        private final Transaction tx;

        @Override
        public int compareTo(TransactionInfo o) {
            int cmp = tx.getFrom().compareTo(o.tx.getFrom());
            if (cmp != 0) return cmp;
            cmp = Long.compare(tx.getNonce(), o.tx.getNonce());
            if (cmp != 0) return cmp;
            cmp = -tx.getGasPrice().compareTo(o.tx.getGasPrice());
            if (cmp != 0) return cmp;
            return tx.getHash().compareTo(o.tx.getHash());
        }
    }
}
