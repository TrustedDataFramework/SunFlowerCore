package org.tdf.sunflower.facade;

import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.types.ValidateResult;

public interface Validator extends BlockValidator, PendingTransactionValidator {
    Validator NONE = new Validator() {
        @Override
        public ValidateResult validate(Block block, Block dependency) {
            return ValidateResult.success();
        }

        @Override
        public ValidateResult validate(Block dependency, Transaction transaction) {
            return ValidateResult.success();
        }
    };
}
