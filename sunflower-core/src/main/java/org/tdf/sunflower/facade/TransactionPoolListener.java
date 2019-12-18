package org.tdf.sunflower.facade;

import org.tdf.sunflower.types.Transaction;

public interface TransactionPoolListener {
    void onNewTransactionCollected(Transaction transaction);
}
