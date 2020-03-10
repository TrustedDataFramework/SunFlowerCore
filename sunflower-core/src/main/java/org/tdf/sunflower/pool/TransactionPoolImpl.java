package org.tdf.sunflower.pool;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tdf.common.event.EventBus;
import org.tdf.common.store.Store;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.TransactionPoolConfig;
import org.tdf.sunflower.controller.PageSize;
import org.tdf.sunflower.events.NewBestBlock;
import org.tdf.sunflower.events.NewTransactionsCollected;
import org.tdf.sunflower.events.NewTransactionsReceived;
import org.tdf.sunflower.facade.ConsensusEngineFacade;
import org.tdf.sunflower.facade.PendingTransactionValidator;
import org.tdf.sunflower.facade.TransactionPool;
import org.tdf.sunflower.facade.TransactionRepository;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.types.PagedView;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.types.ValidateResult;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Slf4j(topic = "txPool")
@Component
public class TransactionPoolImpl implements TransactionPool {
    enum PendingTransactionState {
        /**
         * Transaction may be dropped due to:
         * - Invalid transaction (invalid nonce, low gas price, insufficient account funds,
         * invalid signature)
         * - Timeout (when pending transaction is not included to any block for
         * last [transaction.outdated.threshold] blocks
         * This is the final state
         */
        DROPPED,

        /**
         * The same as PENDING when transaction is just arrived
         * Next state can be either PENDING or INCLUDED
         */
        NEW_PENDING,

        /**
         * State when transaction is not included to any blocks (on the main chain), and
         * was executed on the last best block. The repository state is reflected in the PendingState
         * Next state can be either INCLUDED, DROPPED (due to timeout)
         * or again PENDING when a new block (without this transaction) arrives
         */
        PENDING,

        /**
         * State when the transaction is included to a block.
         * This could be the final state, however next state could also be
         * PENDING: when a fork became the main chain but doesn't include this tx
         * INCLUDED: when a fork became the main chain and tx is included into another
         * block from the new main chain
         * DROPPED: If switched to a new (long enough) main chain without this Tx
         */
        INCLUDED;

        public boolean isPending() {
            return this == NEW_PENDING || this == PENDING;
        }
    }

    private final EventBus eventBus;

    private final TreeSet<TransactionInfo> cache;

    private PendingTransactionValidator validator;

    private final ScheduledExecutorService poolExecutor;

    private final TransactionPoolConfig config;

    private final TransactionRepository transactionRepository;

    private ReadWriteLock cacheLock = new ReentrantReadWriteLock();

    // dropped transactions
    private Cache<HexBytes, Transaction> dropped;

    public void setEngine(ConsensusEngineFacade engine) {
        this.validator = engine.getValidator();
    }

    @AllArgsConstructor
    static class TransactionInfo implements Comparable<TransactionInfo> {
        private long receivedAt;
        private Transaction tx;

        @Override
        public int compareTo(TransactionInfo o) {
            int cmp = tx.getFrom().compareTo(o.tx.getFrom());
            if (cmp != 0) return cmp;
            cmp = Long.compare(tx.getNonce(), o.tx.getNonce());
            if (cmp != 0) return cmp;
            cmp = -Long.compare(tx.getGasPrice(), o.tx.getGasPrice());
            if (cmp != 0) return cmp;
            return tx.getHash().compareTo(o.tx.getHash());
        }
    }

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
    }

    @SneakyThrows
    private void clear() {
        long now = System.currentTimeMillis();
        if (!this.cacheLock.writeLock().tryLock(config.getLockTimeout(), TimeUnit.SECONDS)) {
            return;
        }
        try {
            cache.removeIf(info -> {
                boolean remove = (now - info.receivedAt) / 1000 > config.getExpiredIn();
                if (remove && !transactionRepository.containsTransaction(info.tx.getHash().getBytes())) {
                    dropped.put(info.tx.getHash(), info.tx);
                }
                return remove;
            });
        } finally {
            this.cacheLock.writeLock().unlock();
        }
    }

    @Override
    @SneakyThrows
    public void collect(Collection<? extends Transaction> transactions) {
        eventBus.publish(new NewTransactionsReceived(new ArrayList<>(transactions)));
        this.cacheLock.writeLock().lock();
        try {
            List<Transaction> newCollected = new ArrayList<>(transactions.size());
            for (Transaction transaction : transactions) {
                TransactionInfo info = new TransactionInfo(System.currentTimeMillis(), transaction);
                if (cache.contains(info) || dropped.asMap().containsKey(transaction.getHash()))
                    continue;
                ValidateResult res = transaction.basicValidate();
                if (!res.isSuccess()) {
                    log.error(res.getReason());
                    continue;
                }
                res = validator.validate(transaction);
                if (!res.isSuccess()) {
                    log.error(res.getReason());
                    continue;
                }
                if (validator.validate(transaction).isSuccess()) {
                    cache.add(info);
                    newCollected.add(transaction);
                }
            }
            eventBus.publish(new NewTransactionsCollected(newCollected));
        } finally {
            this.cacheLock.writeLock().unlock();
        }
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
                    if (!transactionRepository.containsTransaction(t.getHash().getBytes()))
                        dropped.put(t.getHash(), t);
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
                    cache.remove(new TransactionInfo(System.currentTimeMillis(), t))
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
}
