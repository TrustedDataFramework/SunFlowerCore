package org.tdf.sunflower.facade;

import org.tdf.common.types.BlockConfirms;
import org.tdf.sunflower.types.Transaction;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    TransactionRepository NONE = new TransactionRepository() {
        @Override
        public boolean containsTransaction(byte[] hash) {
            return false;
        }


        @Override
        public Optional<Transaction> getTransactionByHash(byte[] hash) {
            return Optional.empty();
        }


        @Override
        public List<Transaction> getTransactionsByBlockHash(byte[] blockHash) {
            return Collections.emptyList();
        }

//        @Override
//        public void traverseTransactions(BiFunction<byte[], Transaction, Boolean> traverser) {
//
//        }
    };

    boolean containsTransaction(byte[] hash);

    Optional<Transaction> getTransactionByHash(byte[] hash);

    List<Transaction> getTransactionsByBlockHash(byte[] blockHash);

//    void traverseTransactions(BiFunction<byte[], Transaction, Boolean> traverser);
}
