package org.tdf.sunflower.consensus.pow;

import org.tdf.sunflower.consensus.AbstractValidator;
import org.tdf.sunflower.consensus.poa.PoAConstants;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.types.ValidateResult;

public class PoWValidator extends AbstractValidator {
    private final PoW poW;

    public PoWValidator(PoW pow) {
        super(pow.getAccountTrie());
        this.poW = pow;
    }

    @Override
    public ValidateResult validate(Block block, Block dependency) {
        ValidateResult res = super.commonValidate(block, dependency);
        if (!res.isSuccess()) return res;

        if (block.getVersion() != PoW.BLOCK_VERSION) {
            return ValidateResult.fault("version not match");
        }
        byte[] nbits = poW.getNBits(dependency.getStateRoot().getBytes());
        if (PoW.compare(PoW.getPoWHash(block), nbits) > 0)
            return ValidateResult.fault("nbits");
        return ValidateResult.success();
    }

    @Override
    public ValidateResult validate(Transaction transaction) {
        if (transaction.getVersion() != PoAConstants.TRANSACTION_VERSION) {
            return ValidateResult.fault("transaction version not match");
        }
        return ValidateResult.success();
    }
}
