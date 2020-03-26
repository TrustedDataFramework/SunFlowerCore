package org.tdf.sunflower.state;

import org.tdf.common.trie.Trie;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

public interface BiosContractUpdater {
    Account getGenesisAccount();

    default void update(
            Header header, Transaction transaction, Account account, Trie<byte[], byte[]> contractStorage){
        if(
                transaction.getType() != Transaction.Type.CONTRACT_CALL.code
                        || !transaction.getTo().equals(getGenesisAccount().getAddress()))
            throw new IllegalArgumentException();
    }
}
