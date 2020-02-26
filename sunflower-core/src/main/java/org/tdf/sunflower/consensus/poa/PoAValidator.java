package org.tdf.sunflower.consensus.poa;

import lombok.Setter;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.facade.Validator;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.AccountTrie;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.types.ValidateResult;

@Setter
public class PoAValidator implements Validator {
    private StateTrie<HexBytes, Account> accountTrie;

    @Override
    public ValidateResult validate(Block block, Block dependency) {
        if(!dependency.isParentOf(block)) {
            return ValidateResult.fault("dependency is not parent of block");
        }
        if (dependency.getHeight() + 1 != block.getHeight()){
            return ValidateResult.fault("block height not increase strictly");
        }
        if (block.getVersion() != PoAConstants.BLOCK_VERSION){
            return ValidateResult.fault("version not match");
        }
        if(!Transaction.getTransactionsRoot(block.getBody())
                .equals(block.getTransactionsRoot())
        ){
            return ValidateResult.fault("transactions root not match");
        }
        try{
            Trie<HexBytes, Account> updated =
                    accountTrie.update(dependency.getStateRoot().getBytes(), block);
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

    @Override
    public ValidateResult validate(Transaction transaction) {
        if(transaction.getVersion() != PoAConstants.TRANSACTION_VERSION){
            return ValidateResult.fault("transaction version not match");
        }
        return ValidateResult.success();
    }
}
