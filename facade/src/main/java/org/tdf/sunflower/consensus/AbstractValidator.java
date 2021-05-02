package org.tdf.sunflower.consensus;

import lombok.NonNull;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.ByteUtil;
import org.tdf.common.util.HexBytes;
import org.tdf.common.util.SafeMath;
import org.tdf.sunflower.facade.Validator;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.*;
import org.tdf.sunflower.vm.Backend;
import org.tdf.sunflower.vm.CallData;
import org.tdf.sunflower.vm.VMExecutor;

import java.util.*;

public abstract class AbstractValidator implements Validator {
    protected final StateTrie<HexBytes, Account> accountTrie;

    public AbstractValidator(StateTrie<HexBytes, Account> accountTrie) {
        this.accountTrie = accountTrie;
    }

    protected BlockValidateResult commonValidate(@NonNull Block block, @NonNull Block parent) {
        if (block.getBody() == null || block.getBody().isEmpty())
            return BlockValidateResult.fault("missing block body");

        // a block should contains exactly one coin base transaction

        for (Transaction t : block.getBody()) {
            // TODO: transaction basic validte
//            ValidateResult res = t.basicValidate();
//            if (!res.isSuccess()) return BlockValidateResult.fault(res.getReason());
        }
        if (!parent.isParentOf(block) || parent.getHeight() + 1 != block.getHeight()) {
            return BlockValidateResult.fault("dependency is not parent of block");
        }
        if (parent.getCreatedAt() >= block.getCreatedAt()) {
            return BlockValidateResult.fault(
                    String.format("invalid timestamp %d at block height %d", block.getCreatedAt(), block.getHeight())
            );
        }
        if (!Transaction.calcTxTrie(block.getBody()).equals(
                block.getTransactionsRoot()
        )) {
            return BlockValidateResult.fault("transactions root not match");
        }
        BlockValidateResult success = BlockValidateResult.success();

        Uint256 totalFee = Uint256.ZERO;
        long gas = 0;

        Map<HexBytes, VMResult> results = new HashMap<>();
        HexBytes currentRoot = parent.getStateRoot();
        long currentGas = 0;
        List<TransactionReceipt> receipts = new ArrayList<>();

        try {
            Backend tmp = accountTrie.createBackend(parent.getHeader(), null, false);
            Transaction coinbase = block.getBody().get(0);


            for (Transaction tx : block.getBody().subList(1, block.getBody().size())) {
                VMExecutor executor = new VMExecutor(tmp, CallData.fromTransaction(tx, false));
                VMResult r = executor.execute();
                results.put(HexBytes.fromBytes(tx.getHash()), r);
                totalFee = totalFee.safeAdd(r.getFee());
                // todo:

                currentGas += r.getGasUsed();
                TransactionReceipt receipt = new TransactionReceipt(
                        currentRoot.getBytes(),
                        ByteUtil.longToBytesNoLeadZeroes(currentGas),
                        new Bloom(),
                        Collections.emptyList()
                );
                receipt.setGasUsed(r.getGasUsed());
                receipt.setExecutionResult(r.getExecutionResult());
                currentRoot = tmp.merge();
                receipts.add(receipt);
                tmp = accountTrie.createBackend(parent.getHeader(), currentRoot, null, false);
            }

            tmp.setHeaderCreatedAt(block.getCreatedAt());
            VMExecutor executor = new VMExecutor(tmp, CallData.fromTransaction(coinbase, true));
            VMResult r = executor.execute();
            currentGas += r.getGasUsed();

            TransactionReceipt receipt = new TransactionReceipt(
                    currentRoot.getBytes(),
                    ByteUtil.longToBytesNoLeadZeroes(currentGas),
                    new Bloom(),
                    Collections.emptyList()
            );
            receipt.setGasUsed(r.getGasUsed());
            receipt.setExecutionResult(r.getExecutionResult());
            receipts.add(0, receipt);

            HexBytes rootHash = tmp.merge();
            if (!rootHash.equals(block.getStateRoot())) {
                return BlockValidateResult.fault("state root not match");
            }
            success.setEvents(tmp.getEvents());
        } catch (Exception e) {
            e.printStackTrace();
            return BlockValidateResult.fault("contract evaluation failed or " + e.getMessage());
        }

        List<TransactionInfo> infos = new ArrayList<>();
        for (int i = 0; i < receipts.size(); i++) {
            infos.add(new TransactionInfo(receipts.get(i), block.getHash().getBytes(), i));
        }
        success.setInfos(infos);
        success.setResults(results);
        success.setFee(totalFee);
        success.setGas(gas);
        return success;
    }
}
