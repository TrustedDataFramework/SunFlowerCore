package org.tdf.sunflower.events;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.tdf.sunflower.types.Transaction;

import java.util.List;

@Value
@AllArgsConstructor
public class NewTransactionsCollected {
    private List<Transaction> transactions;
}
