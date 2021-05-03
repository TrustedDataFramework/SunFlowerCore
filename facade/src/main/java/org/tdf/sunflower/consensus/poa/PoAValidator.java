package org.tdf.sunflower.consensus.poa;

import lombok.Setter;
import lombok.SneakyThrows;
import org.tdf.common.crypto.ECDSASignature;
import org.tdf.common.crypto.ECKey;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.ByteUtil;
import org.tdf.common.util.HexBytes;
import org.tdf.common.util.RLPUtil;
import org.tdf.rlp.RLPElement;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.consensus.AbstractValidator;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.*;

import java.math.BigInteger;
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
    @SneakyThrows
    public ValidateResult validate(Block block, Block dependency) {
        BlockValidateResult res = super.commonValidate(block, dependency);
        if (!res.isSuccess()) return res;

        Uint256 fee = res.getFee();
        if (!fee.safeAdd(poA.economicModel.getConsensusRewardAtHeight(dependency.getHeight() + 1)).equals(block.getBody().get(0).getValueAsUint())) {
            return ValidateResult.fault("reward of coin base transaction should be " + poA.economicModel.getConsensusRewardAtHeight(dependency.getHeight() + 1));
        }

        ValidateResult res0 = validateCoinBase(dependency, block.getBody().get(0));
        if (!res0.isSuccess())
            return res0;

        if (
                !poA.getProposer(dependency, block.getCreatedAt())
                        .map(x -> x.getAddress().equals(block.getBody().get(0).getReceiveHex()))
                        .orElse(false)
        ) return ValidateResult.fault("invalid proposer " + block.getBody().get(0).getSenderHex());

        // validate signature
        RLPList vrs = RLPElement.fromEncoded(block.getExtraData().getBytes()).asRLPList();
        byte v = vrs.get(0).asByte();
        byte[] r = vrs.get(1).asBytes();
        byte[] s = vrs.get(2).asBytes();
        ECDSASignature signature = ECDSASignature.fromComponents(r, s, v);

        byte[] rawHash = PoaUtils.getRawHash(block.getHeader());
        // validate signer
        HexBytes signer = HexBytes.fromBytes(ECKey.signatureToAddress(rawHash, signature));

        if(!signer.equals(block.getCoinbase())) {
            return ValidateResult.fault("signer not equals to coinbase");
        }

        ECKey key = ECKey.signatureToKey(rawHash, signature);
        // verify signature
        if(!key.verify(rawHash, signature)) {
            return ValidateResult.fault("verify signature failed");
        }

        return res;
    }

    // validate pre-pending transaction
    @Override
    public ValidateResult validate(Header dependency, Transaction transaction) {
        if(!poA.getPoAConfig().isControlled())
            return ValidateResult.success();

        HexBytes farmBaseAdmin = HexBytes.fromHex(poA.getPoAConfig().getFarmBaseAdmin());

        if (poA.getPoAConfig().getThreadId() == PoA.GATEWAY_ID) {// for gateway node, only accept transaction from farm-base admin
            if (Objects.requireNonNull(transaction.getChainId()) != PoA.GATEWAY_ID || !transaction.getSenderHex().equals(farmBaseAdmin)) {
                return ValidateResult.fault(
                    String.format(
                        "farmbase only accept admin transaction with network id = %s, while from = %s, network id = %s",
                        PoA.GATEWAY_ID,
                        transaction.getSenderHex(),
                        transaction.getChainId()
                    )
                );
            }

            // for thread node, only accept transaction with thread id or gateway id
        } else {
            if (transaction.getChainId() != PoA.GATEWAY_ID && transaction.getChainId() != poA.getPoAConfig().getThreadId()) {
                return ValidateResult.fault(
                    String.format(
                        "this thread only accept transaction with thread id = %s, while id = %s received",
                        poA.getPoAConfig().getThreadId(),
                        transaction.getChainId()
                    )
                );
            }

            if (transaction.getChainId() == PoA.GATEWAY_ID && !transaction.getSenderHex().equals(farmBaseAdmin)) {
                return ValidateResult.fault("transaction with zero version should received from farmbase");
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
