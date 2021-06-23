package org.tdf.sunflower.facade;

import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.types.ValidateResult;

public interface PendingTransactionValidator {
    ValidateResult validate(RepositoryReader rd, Header dependency, Transaction transaction);
}
