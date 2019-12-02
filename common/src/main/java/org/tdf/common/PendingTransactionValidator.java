package org.tdf.common;

public interface PendingTransactionValidator {
    ValidateResult validate(Transaction transaction);
}
