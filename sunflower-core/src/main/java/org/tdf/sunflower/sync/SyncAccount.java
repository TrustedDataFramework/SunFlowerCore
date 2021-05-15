package org.tdf.sunflower.sync;

import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.state.Account;

import java.util.List;

public class SyncAccount {
    // the account itself
    private Account account;
    // contract code of contract account
    private HexBytes contractCode;
    // the contract storage of the account
    private List<HexBytes> contractStorage;

    public SyncAccount(Account account, HexBytes contractCode, List<HexBytes> contractStorage) {
        this.account = account;
        this.contractCode = contractCode;
        this.contractStorage = contractStorage;
    }

    public SyncAccount() {
    }

    public Account getAccount() {
        return this.account;
    }

    public HexBytes getContractCode() {
        return this.contractCode;
    }

    public List<HexBytes> getContractStorage() {
        return this.contractStorage;
    }
}
