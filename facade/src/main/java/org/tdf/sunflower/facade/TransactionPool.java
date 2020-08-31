package org.tdf.sunflower.facade;

import org.tdf.common.store.Store;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.types.PageSize;
import org.tdf.sunflower.types.PagedView;
import org.tdf.sunflower.types.Transaction;

import java.util.*;

public interface TransactionPool{

    // collect transactions into transaction pool
    List<String> collect(Collection<? extends Transaction> transactions);

    default void collect(Transaction tx){
        collect(Collections.singleton(tx));
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

    default Optional<Transaction> get(HexBytes hash){
        return Optional.empty();
    }


}
