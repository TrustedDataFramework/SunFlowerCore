package org.tdf.sunflower.sync;

import org.tdf.common.util.HexBytes;

public class GetAccounts {
    private HexBytes stateRoot;
    private int maxAccounts;

    public GetAccounts(HexBytes stateRoot, int maxAccounts) {
        this.stateRoot = stateRoot;
        this.maxAccounts = maxAccounts;
    }

    public GetAccounts() {
    }

    public HexBytes getStateRoot() {
        return this.stateRoot;
    }

    public int getMaxAccounts() {
        return this.maxAccounts;
    }
}
