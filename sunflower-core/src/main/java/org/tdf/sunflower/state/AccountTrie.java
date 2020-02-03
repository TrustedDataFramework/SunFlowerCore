package org.tdf.sunflower.state;

import org.tdf.common.serialize.Codecs;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.db.DatabaseStoreFactory;
import org.tdf.sunflower.types.Block;

public class AccountTrie extends AbstractStateTrie<HexBytes, Account> {
    public AccountTrie(StateUpdater<HexBytes, Account> updater, DatabaseStoreFactory factory) {
        super(updater, Codecs.newRLPCodec(HexBytes.class), Codecs.newRLPCodec(Account.class), factory);
    }

    @Override
    protected String getPrefix() {
        return "account";
    }
}
