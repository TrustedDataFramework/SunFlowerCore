package org.tdf.sunflower.events;

import lombok.Value;
import org.tdf.sunflower.types.Transaction;

@Value
public class TransactionConfirmed {
    Transaction tx;
}
