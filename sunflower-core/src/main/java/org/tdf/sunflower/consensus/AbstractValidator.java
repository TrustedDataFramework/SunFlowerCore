package org.tdf.sunflower.consensus;

import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.facade.Validator;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.types.ValidateResult;

public abstract class AbstractValidator implements Validator {
    protected final StateTrie<HexBytes, Account> accountTrie;

    public AbstractValidator(StateTrie<HexBytes, Account> accountTrie){
        this.accountTrie = accountTrie;
    }

    protected ValidateResult commonValidate(Block block, Block parent){
        if(!parent.isParentOf(block) || parent.getHeight() + 1 != block.getHeight()) {
            return ValidateResult.fault("dependency is not parent of block");
        }
        if(!Transaction.getTransactionsRoot(block.getBody())
                .equals(block.getTransactionsRoot())
        ){
            return ValidateResult.fault("transactions root not match");
        }
        try{
            Trie<HexBytes, Account> updated =
                    accountTrie.update(parent.getStateRoot().getBytes(), block);
            if(!HexBytes.fromBytes(updated.getRootHash()).equals(block.getStateRoot())){
                return ValidateResult.fault("state root not match");
            }
            updated.flush();
        }catch (Exception e){
            e.printStackTrace();
            return ValidateResult.fault("contract evaluation failed or " + e.getMessage());
        }
        return ValidateResult.success();
    }
}
