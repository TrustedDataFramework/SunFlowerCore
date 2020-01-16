package org.tdf.sunflower.pool;

import com.google.common.cache.Weigher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.facade.*;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.types.ValidateResult;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
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

    private static class TransactionWeigher implements Weigher<HexBytes, Transaction> {
        @Override
        public int weigh(HexBytes key, Transaction value) {
            return value.size();
        }
    }

    private HashPolicy hashPolicy;

    private final TreeSet<Transaction> cache;

    private PendingTransactionValidator validator;

    private List<TransactionPoolListener> listeners = new CopyOnWriteArrayList<>();

    public void setEngine(ConsensusEngine engine) {
        this.hashPolicy = engine.getHashPolicy();
        this.validator = engine.getValidator();
    }

    public TransactionPoolImpl() {
        cache = new TreeSet<>((a, b) -> {
            if (a.getNonce() != b.getNonce()) return Long.compare(a.getNonce(), b.getNonce());
            return a.getHash().compareTo(b.getHash());
        });
    }

    @Override
    public void collect(Collection<? extends Transaction> transactions) {
        for (Transaction transaction : transactions) {
            if (transaction.getHash() == null) {
                transaction.setHash(hashPolicy.getHash(transaction));
            }
            ValidateResult res = validator.validate(transaction);
            if (!res.isSuccess()) {
                log.error(res.getReason());
                continue;
            }
            synchronized (cache) {
                if (cache.contains(transaction)) continue;
                if (validator.validate(transaction).isSuccess()) {
                    cache.add(transaction);
                    listeners.forEach(c -> c.onNewTransactionCollected(transaction));
                }
            }
        }
    }

    @Override
    public Optional<Transaction> pop() {
        synchronized (cache) {
            if (cache.isEmpty()) {
                return Optional.empty();
            }

            Transaction tx = cache.first();
            cache.remove(tx);
            return Optional.of(tx);
        }
    }

    @Override
    public List<Transaction> pop(int limit) {
        synchronized (cache) {

            List<Transaction> ret = cache
                    .stream()
                    .limit(limit < 0 ? Long.MAX_VALUE : limit)
                    .collect(Collectors.toList());

            ret.forEach(cache::remove);
            return ret;
        }
    }

    @Override
    public int size() {
        return cache.size();
    }

    @Override
    public List<Transaction> get(int page, int size) {
        synchronized (cache) {
            return cache.stream()
                    .skip(page * size)
                    .limit(size)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public void setValidator(PendingTransactionValidator validator) {
        this.validator = validator;
    }

    @Override
    public void addListeners(TransactionPoolListener... listeners) {
        this.listeners.addAll(Arrays.asList(listeners));
    }

    @Override
    public void onBlockWritten(Block block) {

    }

    @Override
    public void onNewBestBlock(Block block) {
        synchronized (cache) {
            block.getBody().forEach(cache::remove);
        }
    }

    @Override
    public void onBlockConfirmed(Block block) {

    }


    @Override
    public void onBlockMined(Block block) {

    }

    @Override
    public void onMiningFailed(Block block) {
        collect(block.getBody());
    }
}
