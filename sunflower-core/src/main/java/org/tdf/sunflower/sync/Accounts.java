package org.tdf.sunflower.sync;

import java.util.List;

public class Accounts {
    private int total;
    private List<SyncAccount> accounts;
    private boolean traversed;

    public Accounts(int total, List<SyncAccount> accounts, boolean traversed) {
        this.total = total;
        this.accounts = accounts;
        this.traversed = traversed;
    }

    public Accounts() {
    }

    public int getTotal() {
        return this.total;
    }

    public List<SyncAccount> getAccounts() {
        return this.accounts;
    }

    public boolean isTraversed() {
        return this.traversed;
    }
}
