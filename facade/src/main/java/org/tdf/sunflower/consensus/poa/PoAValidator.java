package org.tdf.sunflower.consensus.poa;

import lombok.Setter;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.consensus.AbstractValidator;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.*;

import java.util.Objects;

@Setter
public class PoAValidator extends AbstractValidator {
    public static final HexBytes FARM_BASE_ADMIN = HexBytes.fromHex("");

    private final PoA poA;

    public PoAValidator(StateTrie<HexBytes, Account> accountTrie, PoA poA) {
        super(accountTrie);
        this.poA = poA;
    }

    @Override
    public ValidateResult validate(Block block, Block dependency) {
        BlockValidateResult res = super.commonValidate(block, dependency);
        if (!res.isSuccess()) return res;
        Uint256 fee = res.getFee();
        if (!fee.safeAdd(poA.economicModel.getConsensusRewardAtHeight(dependency.getHeight() + 1)).equals(block.getBody().get(0).getValueAsUint())) {
            return ValidateResult.fault("reward of coin base transaction should be " + poA.economicModel.getConsensusRewardAtHeight(dependency.getHeight() + 1));
        }

        // TODO: validate signature here


        ValidateResult res0 = validateCoinBase(dependency, block.getBody().get(0));
        if (!res0.isSuccess())
            return res0;

        if (
                !poA.getProposer(dependency, block.getCreatedAt())
                        .map(x -> x.getAddress().equals(block.getBody().get(0).getReceiveHex()))
                        .orElse(false)
        ) return ValidateResult.fault("invalid proposer " + block.getBody().get(0).getSenderHex());
        return res;
    }

    @Override
    public ValidateResult validate(Block dependency, Transaction transaction) {
        HexBytes farmBaseAdmin = HexBytes.fromHex(poA.getPoAConfig().getFarmBaseAdmin());

        switch (poA.getPoAConfig().getRole()) {
            case "gateway": {
                // for farm base node, only accept transaction from farm-base admin
                if (Objects.requireNonNull(transaction.getChainId()) != 0 || !transaction.getSenderHex().equals(farmBaseAdmin)) {
                    return ValidateResult.fault(
                            String.format(
                                    "farmbase only accept admin transaction with network id = 0, while from = %s, network id = %s",
                                    transaction.getSenderHex(),
                                    transaction.getChainId()
                            )
                    );
                }
                break;
            }

            // for thread node, only accept transaction with thread id or zero
            case "thread": {
                if (transaction.getChainId() != 0 && transaction.getChainId() != poA.getPoAConfig().getThreadId()) {
                    return ValidateResult.fault(
                            String.format(
                                    "this thread only accept transaction with thread id = %s, while id = %s received",
                                    poA.getPoAConfig().getThreadId(),
                                    transaction.getChainId()
                            )
                    );
                }

                if (transaction.getChainId() == 0 && !transaction.getSenderHex().equals(farmBaseAdmin)) {
                    return ValidateResult.fault("transaction with zero version should received from farmbase");
                }

                break;
            }
            default: {
                throw new RuntimeException("invalid role");
            }
        }


        if (!transaction.getSenderHex().equals(farmBaseAdmin)
                && !poA.getValidators(
                dependency.getStateRoot())
                .contains(transaction.getSenderHex()
                )
        )
            return ValidateResult.fault("from address is not approved");


        return ValidateResult.success();
    }

    private ValidateResult validateCoinBase(Block parent, Transaction coinBase) {

        if (coinBase.getNonceAsLong() != parent.getHeight() + 1)
            return ValidateResult.fault("nonce of coin base should be " + parent.getHeight() + 1);

        return ValidateResult.success();
    }
}
