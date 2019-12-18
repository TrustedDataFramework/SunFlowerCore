package org.tdf.sunflower.consensus.poa;

import org.tdf.sunflower.facade.Validator;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.types.ValidateResult;

public class PoAValidator implements Validator {
    @Override
    public ValidateResult validate(Block block, Block dependency) {
        if (dependency.getHeight() + 1 != block.getHeight()){
            return ValidateResult.fault("block height not increase strictly");
        }
        if (block.getVersion() != PoAConstants.BLOCK_VERSION){
            return ValidateResult.fault("version not match");
        }
        if (!PoAHashPolicy.HASH_POLICY.getHash(block).equals(block.getHash())){
            return ValidateResult.fault("hash not match");
        }
        return ValidateResult.success();
    }

    @Override
    public ValidateResult validate(Transaction transaction) {
        if(transaction.getVersion() != PoAConstants.TRANSACTION_VERSION){
            return ValidateResult.fault("transaction version not match");
        }
        return ValidateResult.success();
    }
}
