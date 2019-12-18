package org.tdf.sunflower.facade;

import org.tdf.sunflower.types.Transaction;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    boolean hasTransaction(byte[] hash);

    boolean hasPayload(byte[] payload);

    Optional<Transaction> getTransactionByHash(byte[] hash);

    List<Transaction> getTransactionsByFrom(byte[] from, int page, int size);

    List<Transaction> getTransactionsByFromAndType(byte[] from, int type, int page, int size);

    List<Transaction> getTransactionsByTo(byte[] to, int page, int size);

    List<Transaction> getTransactionsByToAndType(byte[] to, int type, int page, int size);

    List<Transaction> getTransactionsByFromAndTo(byte[] from, byte[] to, int page, int size);

    List<Transaction> getTransactionsByFromAndToAndType(byte[] from, byte[] to, int type, int page, int size);

    List<Transaction> getTransactionsByPayload(byte[] payload, int page, int size);

    List<Transaction> getTransactionsByPayloadAndType(byte[] payload, int type, int page, int size);

    List<Transaction> getTransactionsByBlockHash(byte[] blockHash, int page, int size);

    List<Transaction> getTransactionsByBlockHeight(long height, int page, int size);

    TransactionRepository NONE = new TransactionRepository() {
        @Override
        public boolean hasTransaction(byte[] hash) {
            return false;
        }

        @Override
        public boolean hasPayload(byte[] payload) {
            return false;
        }

        @Override
        public Optional<Transaction> getTransactionByHash(byte[] hash) {
            return Optional.empty();
        }

        @Override
        public List<Transaction> getTransactionsByFrom(byte[] from, int page, int size) {
            return Collections.emptyList();
        }

        @Override
        public List<Transaction> getTransactionsByFromAndType(byte[] from, int type, int page, int size) {
            return Collections.emptyList();
        }

        @Override
        public List<Transaction> getTransactionsByTo(byte[] to, int page, int size) {
            return Collections.emptyList();
        }

        @Override
        public List<Transaction> getTransactionsByToAndType(byte[] to, int type, int page, int size) {
            return Collections.emptyList();
        }

        @Override
        public List<Transaction> getTransactionsByFromAndTo(byte[] from, byte[] to, int page, int size) {
            return Collections.emptyList();
        }

        @Override
        public List<Transaction> getTransactionsByFromAndToAndType(byte[] from, byte[] to, int type, int page, int size) {
            return Collections.emptyList();
        }

        @Override
        public List<Transaction> getTransactionsByPayload(byte[] payload, int page, int size) {
            return Collections.emptyList();
        }

        @Override
        public List<Transaction> getTransactionsByPayloadAndType(byte[] payload, int type, int page, int size) {
            return Collections.emptyList();
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
