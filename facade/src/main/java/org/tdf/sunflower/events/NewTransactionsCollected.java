package org.tdf.sunflower.events;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.tdf.sunflower.types.Transaction;

import java.util.List;

// when receive new transaction in pools, broadcast to peers
@Value
@AllArgsConstructor
public class NewTransactionsCollected {
    List<Transaction> transactions;
}
