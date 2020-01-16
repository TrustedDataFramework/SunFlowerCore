package org.tdf.sunflower.events;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.tdf.sunflower.types.Transaction;

@Value
@AllArgsConstructor
public class NewTransactionCollected {
    private Transaction transaction;
}
