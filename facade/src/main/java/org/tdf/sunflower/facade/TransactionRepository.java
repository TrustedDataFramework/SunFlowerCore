package org.tdf.sunflower.facade;

import org.tdf.sunflower.types.Transaction;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public interface TransactionRepository {
    boolean containsTransaction(byte[] hash);

    Optional<Transaction> getTransactionByHash(byte[] hash);

    List<Transaction> getTransactionsByBlockHash(byte[] blockHash);

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

        @Override
        public long getConfirms(byte[] transactionHash) {
            return 0;
        }
    };

    long getConfirms(byte[] transactionHash);

    default void traverseTransactions(BiFunction<byte[], Transaction, Boolean> traverser){

    }
}
