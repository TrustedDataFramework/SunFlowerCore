package org.tdf.sunflower.pool;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.Weigher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tdf.common.util.Constants;
import org.tdf.sunflower.facade.*;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.types.ValidateResult;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TransactionPoolImpl implements TransactionPool {
    enum PendingTransactionState {
        /**
         * Transaction may be dropped due to:
         * - Invalid transaction (invalid nonce, low gas price, insufficient account funds,
         *         invalid signature)
         * - Timeout (when pending transaction is not included to any block for
         *         last [transaction.outdated.threshold] blocks
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
         *           block from the new main chain
         * DROPPED: If switched to a new (long enough) main chain without this Tx
         */
        INCLUDED;

        public boolean isPending() {
            return this == NEW_PENDING || this == PENDING;
        }
    }

    private static class TransactionWeigher implements Weigher<String, Transaction> {
        @Override
        public int weigh(String key, Transaction value) {
            return value.size();
        }
    }

    private HashPolicy hashPolicy;

    private Cache<String, Transaction> cache;

    private PendingTransactionValidator validator;

    private List<TransactionPoolListener> listeners = new ArrayList<>();

    public void setEngine(ConsensusEngine engine){
        this.hashPolicy = engine.getHashPolicy();
        this.validator = engine.getValidator();
    }

    public TransactionPoolImpl(){
        cache = CacheBuilder.newBuilder()
                .expireAfterWrite(Duration.ofHours(1))
                .weigher(new TransactionWeigher())
                .maximumWeight(256 * Constants.MEGA_BYTES).build();
    }

    @Override
    public void collect(Transaction... transactions) {
        for(Transaction transaction: transactions){
            if(transaction.getHash() == null){
                transaction.setHash(hashPolicy.getHash(transaction));
            }
            ValidateResult res = validator.validate(transaction);
            if (!res.isSuccess()){
                log.error(res.getReason());
                continue;
            }
            String k = transaction.getHash().toString();
            if (cache.asMap().containsKey(k)) continue;
            if(validator.validate(transaction).isSuccess()){
                cache.put(transaction.getHash().toString(), transaction);
                listeners.forEach(c -> c.onNewTransactionCollected(transaction));
            }
        }
    }

    @Override
    public Optional<Transaction> pop() {
        if (cache.asMap().isEmpty()){
            return Optional.empty();
        }
        Optional<Transaction>
        o = cache.asMap().values().stream().sorted((a, b) -> (int) (a.getNonce() - b.getNonce())).findFirst();
        if (!o.isPresent()) return o;
        cache.asMap().remove(o.get().getHash().toString());
        return o;
    }

    @Override
    public List<Transaction> pop(int limit) {
        List<Transaction> list = new ArrayList<>();
        for(int i = 0; i < limit; i++){
            Optional<Transaction> o = pop();
            if (!o.isPresent()) return list;
            list.add(o.get());
        }
        return list;
    }

    @Override
    public int size() {
        return (int) cache.size();
    }

    @Override
    public List<Transaction> get(int page, int size) {
        return cache.asMap().values().stream()
                .skip(page * size)
                .limit(size)
                .collect(Collectors.toList());
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
        block.getBody().forEach(tx -> cache.asMap().remove(tx.getHash().toString()));
    }

    @Override
    public void onBlockConfirmed(Block block) {

    }


    @Override
    public void onBlockMined(Block block) {

    }

    @Override
    public void onMiningFailed(Block block) {
        block.getBody().forEach(this::collect);
    }
}
