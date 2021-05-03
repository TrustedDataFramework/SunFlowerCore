package org.tdf.sunflower.consensus.pos;

import lombok.Setter;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.consensus.AbstractValidator;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.*;

@Setter
public class PoSValidator extends AbstractValidator {
    private final PoSMiner posMiner;

    public PoSValidator(StateTrie<HexBytes, Account> accountTrie, PoSMiner posMiner) {
        super(accountTrie);
        this.posMiner = posMiner;
    }

    @Override
    public ValidateResult validate(Block block, Block dependency) {
        BlockValidateResult res = super.commonValidate(block, dependency);
        if (!res.isSuccess()) return res;

        if (
                !posMiner.getProposer(dependency, block.getCreatedAt())
                        .map(x -> x.getAddress().equals(block.getBody().get(0).getReceiveHex()))
                        .orElse(false)
        ) return ValidateResult.fault("invalid proposer " + block.getBody().get(0).getSenderHex());
        return res;
    }

    @Override
    public ValidateResult validate(Header dependency, Transaction transaction) {
        return ValidateResult.success();
    }
}
