package org.tdf.sunflower.facade;

import org.tdf.common.store.Store;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.PageSize;
import org.tdf.sunflower.types.PagedView;
import org.tdf.sunflower.types.Transaction;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface TransactionPool {

    // collect transactions into transaction pool
    List<String> collect(Block best, Collection<? extends Transaction> transactions);

    default void collect(Block best, Transaction tx) {
        collect(best, Collections.singleton(tx));
    }


    // pop at most n packable transactions
    // if limit < 0, pop all transactions
    List<Transaction> popPackable(Store<HexBytes, Account> accountStore, int limit);

    // get size of current transaction pool
    int size();

    // get pending transactions
    PagedView<Transaction> get(PageSize pageSize);

    void setValidator(PendingTransactionValidator validator);

    PagedView<Transaction> getDropped(PageSize pageSize);

    void drop(Transaction transaction);

    default Optional<Transaction> get(HexBytes hash) {
        return Optional.empty();
    }


}
