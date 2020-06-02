package org.tdf.sunflower.events;

import lombok.Value;
import org.tdf.sunflower.types.Transaction;

import java.util.List;

@Value
public class NewTransactionsReceived {
    private List<Transaction> transactions;
}
