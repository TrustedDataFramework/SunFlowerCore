package org.tdf.sunflower.consensus;

import lombok.NonNull;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;
import org.tdf.common.util.SafeMath;
import org.tdf.sunflower.facade.Validator;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.*;
import org.tdf.sunflower.vm.Backend;
import org.tdf.sunflower.vm.CallData;
import org.tdf.sunflower.vm.VMExecutor;
import org.tdf.sunflower.vm.hosts.Limit;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractValidator implements Validator {
    protected final StateTrie<HexBytes, Account> accountTrie;

    public AbstractValidator(StateTrie<HexBytes, Account> accountTrie) {
        this.accountTrie = accountTrie;
    }

    protected BlockValidateResult commonValidate(@NonNull Block block, @NonNull Block parent) {
        if (block.getBody() == null || block.getBody().isEmpty())
            return BlockValidateResult.fault("missing block body");

        // a block should contains exactly one coin base transaction
        if (block.getBody().get(0).getType() != Transaction.Type.COIN_BASE.code)
            return BlockValidateResult.fault("the first transaction of block body should be coin base");

        if (block.getBody().stream().filter(t -> t.getType() == Transaction.Type.COIN_BASE.code).count() > 1)
            return BlockValidateResult.fault("the block body contains at most one coin base transaction");

        for (Transaction t : block.getBody()) {
            ValidateResult res = t.basicValidate();
            if (!res.isSuccess()) return BlockValidateResult.fault(res.getReason());
        }
        if (!parent.isParentOf(block) || parent.getHeight() + 1 != block.getHeight()) {
            return BlockValidateResult.fault("dependency is not parent of block");
        }
        if (parent.getCreatedAt() >= block.getCreatedAt()) {
            return BlockValidateResult.fault(
                    String.format("invalid timestamp %d at block height %d", block.getCreatedAt(), block.getHeight())
            );
        }
        if (!Transaction.getTransactionsRoot(block.getBody())
                .equals(block.getTransactionsRoot())
        ) {
            return BlockValidateResult.fault("transactions root not match");
        }
        BlockValidateResult success = BlockValidateResult.success();

        Uint256 totalFee = Uint256.ZERO;
        long gas = 0;

        Map<HexBytes, TransactionResult> results = new HashMap<>();

        try {
            Backend tmp = accountTrie.createBackend(parent.getHeader(), block.getCreatedAt(), false);
            Transaction coinbase = block.getBody().get(0);

            for (Transaction tx : block.getBody().subList(1, block.getBody().size())) {
                VMExecutor executor = new VMExecutor(tmp, CallData.fromTransaction(tx), new Limit(), 0);
                TransactionResult r = executor.execute();
                results.put(tx.getHash(), r);
                totalFee = totalFee.safeAdd(r.getFee());
                // todo:
                gas = SafeMath.add(gas, r.getGasUsed());
            }

            VMExecutor executor = new VMExecutor(tmp, CallData.fromTransaction(coinbase), new Limit(), 0);
            executor.execute();

            byte[] rootHash = tmp.merge();

            if (!HexBytes.fromBytes(rootHash).equals(block.getStateRoot())) {
                return BlockValidateResult.fault("state root not match");
            }
            success.setEvents(tmp.getEvents());
        } catch (Exception e) {
            e.printStackTrace();
            return BlockValidateResult.fault("contract evaluation failed or " + e.getMessage());
        }
        success.setResults(results);
        success.setFee(totalFee);
        success.setGas(gas);
        return success;
    }
}
