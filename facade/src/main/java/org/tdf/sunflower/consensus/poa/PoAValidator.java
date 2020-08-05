package org.tdf.sunflower.consensus.poa;

import lombok.Setter;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.consensus.AbstractValidator;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.Address;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.CryptoContext;
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
        long fee = (long) res.getCtx();
        if (fee + poA.economicModel.getConsensusRewardAtHeight(dependency.getHeight() + 1) != block.getBody().get(0).getAmount()) {
            return ValidateResult.fault("reward of coin base transaction should be " + poA.economicModel.getConsensusRewardAtHeight(dependency.getHeight() + 1));
        }
        if (block.getVersion() != PoAConstants.BLOCK_VERSION) {
            return ValidateResult.fault("version not match");
        }

        byte[] plain =
                PoA.getSignaturePlain(block);
        byte[] pk = block.getBody().get(0).getPayload().getBytes();

        if (!CryptoContext.verify(pk, plain, block.getPayload().getBytes()))
            return ValidateResult.fault("verify signature failed");

        res = validateCoinBase(dependency, block.getBody().get(0));
        if (!res.isSuccess())
            return res;

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

    private ValidateResult validateCoinBase(Block parent, Transaction coinBase) {
        if (!Address.fromPublicKey(coinBase.getPayload()).equals(coinBase.getTo()))
            return ValidateResult.fault("payload is not equals to public key of address " + coinBase.getTo());

        if (coinBase.getNonce() != parent.getHeight() + 1)
            return ValidateResult.fault("nonce of coin base should be " + parent.getHeight() + 1);

        return ValidateResult.success();
    }
}
