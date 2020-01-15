package org.tdf.sunflower.facade;

import org.tdf.sunflower.types.Transaction;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    boolean containsTransaction(byte[] hash);

    Optional<Transaction> getTransactionByHash(byte[] hash);

    List<Transaction> getTransactionsByBlockHash(byte[] blockHash, int page, int size);

    List<Transaction> getTransactionsByBlockHeight(long height, int page, int size);

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
        public List<Transaction> getTransactionsByBlockHash(byte[] blockHash, int page, int size) {
            return Collections.emptyList();
        }

        @Override
        public List<Transaction> getTransactionsByBlockHeight(long height, int page, int size) {
            return Collections.emptyList();
        }
    };
}
