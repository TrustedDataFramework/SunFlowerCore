package org.tdf.sunflower.consensus;

import lombok.NonNull;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.facade.Validator;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.Constants;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.types.ValidateResult;

import java.util.Map;

public abstract class AbstractValidator implements Validator {
    protected final StateTrie<HexBytes, Account> accountTrie;

    public AbstractValidator(StateTrie<HexBytes, Account> accountTrie) {
        this.accountTrie = accountTrie;
    }

    protected ValidateResult commonValidate(@NonNull Block block, @NonNull Block parent) {
        if (block.getBody() == null || block.getBody().isEmpty())
            return ValidateResult.fault("missing block body");

        // a block should contains exactly one coin base transaction
        if (block.getBody().get(0).getType() != Transaction.Type.COIN_BASE.code)
            return ValidateResult.fault("the first transaction of block body should be coin base");

        if (block.getBody().stream().filter(t -> t.getType() == Transaction.Type.COIN_BASE.code).count() > 1)
            return ValidateResult.fault("the block body contains at most one coin base transaction");

        for (Transaction t : block.getBody()) {
            ValidateResult res = t.basicValidate();
            if (!res.isSuccess()) return res;
        }
        if (!parent.isParentOf(block) || parent.getHeight() + 1 != block.getHeight()) {
            return ValidateResult.fault("dependency is not parent of block");
        }
        if (parent.getCreatedAt() >= block.getCreatedAt()) {
            return ValidateResult.fault(
                    String.format("invalid timestamp %d ", block.getCreatedAt())
            );
        }
        if (!Transaction.getTransactionsRoot(block.getBody())
                .equals(block.getTransactionsRoot())
        ) {
            return ValidateResult.fault("transactions root not match");
        }
        ValidateResult success = ValidateResult.success();

        try {
            Map<HexBytes, Account> accounts =
                    accountTrie.tryUpdate(parent.getStateRoot().getBytes(), block);
            Account feeAccount =
                    accounts.remove(Constants.FEE_ACCOUNT_ADDR);
            Trie<HexBytes, Account>  updated = accountTrie.commit(parent.getStateRoot().getBytes(), accounts);
            if (!HexBytes.fromBytes(updated.getRootHash()).equals(block.getStateRoot())) {
                return ValidateResult.fault("state root not match");
            }
            updated.flush();
            success.setCtx(feeAccount.getBalance());
        } catch (Exception e) {
            e.printStackTrace();
            return ValidateResult.fault("contract evaluation failed or " + e.getMessage());
        }
        return success;
    }
}
