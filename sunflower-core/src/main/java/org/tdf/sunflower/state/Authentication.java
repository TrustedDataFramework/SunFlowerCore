package org.tdf.sunflower.state;

import org.tdf.common.store.Store;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.util.Map;

/**
 * used for node join/exit
 */
public class Authentication implements PreBuiltContract {
    @Override
    public Account getGenesisAccount() {
        return Account.emptyContract(Constants.AUTHENTICATION_ADDR);
    }

    @Override
    public void update(Header header, Transaction transaction, Map<HexBytes, Account> accounts, Store<byte[], byte[]> contractStorage) {

    }

    @Override
    public Map<byte[], byte[]> getGenesisStorage() {
        return null;
    }
}
