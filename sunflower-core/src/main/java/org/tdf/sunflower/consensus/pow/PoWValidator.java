package org.tdf.sunflower.consensus.pow;

import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.consensus.AbstractValidator;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.BlockValidateResult;
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
        BlockValidateResult res = super.commonValidate(block, dependency);
        if (!res.isSuccess()) return res;

        Uint256 nbits = poW.getNBits(dependency.getStateRoot());
        if (PoW.compare(PoW.getPoWHash(block), nbits.getData()) > 0)
            return ValidateResult.fault(
                    String.format(
                            "nbits validate failed hash = %s, nbits = %s",
                            HexBytes.fromBytes(PoW.getPoWHash(block)),
                            HexBytes.fromBytes(nbits.getData())
                    )
            );
        return res;
    }

    @Override
    public ValidateResult validate(Block dependency, Transaction transaction) {
        return ValidateResult.success();
    }
}
