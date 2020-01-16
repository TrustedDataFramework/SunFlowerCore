package org.tdf.sunflower.facade;

import org.tdf.sunflower.types.Transaction;

@Deprecated
// subscribe to event bus instead
/**
 * @see org.tdf.common.event.EventBus
 */
public interface TransactionPoolListener {
    void onNewTransactionCollected(Transaction transaction);
}
