package org.tdf.sunflower.consensus.vrf;

import org.slf4j.Logger;
import org.tdf.common.*;

public class VrfValidator implements ConsensusEngine.Validator {
    @Override
    public ValidateResult validate(Block block, Block dependency) {
        if (dependency.getHeight() + 1 != block.getHeight()){
            return ValidateResult.fault("block height not increase strictly");
        }
//        if (block.getVersion() != PoAConstants.BLOCK_VERSION){
//            return ValidateResult.fault("version not match");
//        }
//        if (!PoAHashPolicy.HASH_POLICY.getHash(block).equals(block.getHash())){
//            return ValidateResult.fault("hash not match");
//        }
        return ValidateResult.success();
    }

    @Override
    public ValidateResult validate(Transaction transaction) {
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
