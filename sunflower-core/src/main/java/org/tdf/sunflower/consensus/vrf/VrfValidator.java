package org.tdf.sunflower.consensus.vrf;

import org.slf4j.Logger;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.consensus.AbstractValidator;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.types.ValidateResult;

public class VrfValidator extends AbstractValidator {
    public VrfValidator(StateTrie<HexBytes, Account> accountTrie) {
        super(accountTrie);
    }

    @Override
    public ValidateResult validate(Block block, Block dependency) {
        ValidateResult res = super.commonValidate(block, dependency);
        if (!res.isSuccess()) return res;

        if (dependency.getHeight() + 1 != block.getHeight()) {
            return ValidateResult.fault("block height not increase strictly");
        }
//        if (block.getVersion() != PoAConstants.BLOCK_VERSION){
//            return ValidateResult.fault("version not match");
//        }
//        if (!PoAHashPolicy.HASH_POLICY.getHash(block).equals(block.getHash())){
//            return ValidateResult.fault("hash not match");
//        }
        return res;
    }

    @Override
    public ValidateResult validate(Block dependency, Transaction transaction) {
//        if(transaction.getVersion() != PoAConstants.TRANSACTION_VERSION){
//            return ValidateResult.fault("transaction version not match");
//        }
        return ValidateResult.success();
    }

    public boolean validateAndLog(Header header, Logger logger) {
//        ValidationResult result = validate(header);
//        if (!result.isSuccess && logger.isErrorEnabled()) {
//            logger.warn("{} invalid {}", getEntityClass(), result.error);
//        }
//        return result.isSuccess;

        return true;
    }
}
