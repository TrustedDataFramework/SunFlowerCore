package org.tdf.sunflower.consensus.poa;

import lombok.Setter;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.consensus.AbstractValidator;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.types.ValidateResult;

@Setter
public class PoAValidator extends AbstractValidator {
    private final PoA poA;

    public PoAValidator(StateTrie<HexBytes, Account> accountTrie, PoA poA) {
        super(accountTrie);
        this.poA = poA;
    }

    @Override
    public ValidateResult validate(Block block, Block dependency) {
        ValidateResult res = super.commonValidate(block, dependency);
        if (!res.isSuccess()) return res;

        if (block.getVersion() != PoAConstants.BLOCK_VERSION) {
            return ValidateResult.fault("version not match");
        }
        if (
                !poA.getProposer(dependency, block.getCreatedAt())
                        .map(x -> x.getAddress().equals(block.getBody().get(0).getTo()))
                        .orElse(false)
        ) return ValidateResult.fault("invalid proposer " + block.getBody().get(0).getFromAddress());
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
