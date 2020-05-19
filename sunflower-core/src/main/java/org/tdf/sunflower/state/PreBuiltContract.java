package org.tdf.sunflower.state;

import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.util.Collections;
import java.util.Map;

public interface PreBuiltContract extends CommonUpdater{
    default void update(
            Header header, Transaction transaction, Map<HexBytes,Account> accounts, Store<byte[], byte[]> contractStorage){
        if(
                transaction.getType() != Transaction.Type.CONTRACT_CALL.code
                        || !transaction.getTo().equals(getGenesisAccount().getAddress()))
            throw new IllegalArgumentException();
    }
}
