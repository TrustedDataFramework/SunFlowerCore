package org.tdf.sunflower.consensus.pow;

import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.consensus.AbstractValidator;
import org.tdf.sunflower.facade.RepositoryReader;
import org.tdf.sunflower.types.*;

public class PoWValidator extends AbstractValidator {
    private final PoW poW;

    public PoWValidator(PoW pow) {
        super(pow.getAccountTrie());
        this.poW = pow;
    }

    @Override
    public ValidateResult validate(RepositoryReader rd, Block block, Block dependency) {
        BlockValidateResult res = super.commonValidate(rd, block, dependency);
        if (!res.getSuccess()) return res;

        Uint256 nbits = poW.bios.getNBits(rd, dependency.getHash());
        if (PoW.compare(PoW.getPoWHash(block), nbits.getByte32()) > 0)
            return ValidateResult.fault(
                String.format(
                    "nbits validate failed hash = %s, nbits = %s",
                    HexBytes.fromBytes(PoW.getPoWHash(block)),
                    HexBytes.fromBytes(nbits.getByte32())
                )
            );
        return res;
    }

    @Override
    public int getChainId() {
        throw new UnsupportedOperationException();
    }
}
