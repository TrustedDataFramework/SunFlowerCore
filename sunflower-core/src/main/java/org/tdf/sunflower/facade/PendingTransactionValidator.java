package org.tdf.sunflower.facade;

import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.types.ValidateResult;

public interface PendingTransactionValidator {
    ValidateResult validate(Transaction transaction);
}
