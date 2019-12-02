package org.tdf.common;

public interface TransactionPoolListener {
    void onNewTransactionCollected(Transaction transaction);
}
