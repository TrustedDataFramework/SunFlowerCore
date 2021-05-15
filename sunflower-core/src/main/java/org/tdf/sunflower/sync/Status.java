package org.tdf.sunflower.sync;

import org.tdf.common.util.HexBytes;


public class Status {
    private long bestBlockHeight;
    private HexBytes bestBlockHash;
    private HexBytes genesisBlockHash;
    private long prunedHeight;
    private HexBytes prunedHash;

    public Status(long bestBlockHeight, HexBytes bestBlockHash, HexBytes genesisBlockHash, long prunedHeight, HexBytes prunedHash) {
        this.bestBlockHeight = bestBlockHeight;
        this.bestBlockHash = bestBlockHash;
        this.genesisBlockHash = genesisBlockHash;
        this.prunedHeight = prunedHeight;
        this.prunedHash = prunedHash;
    }

    public Status() {
    }

    public long getBestBlockHeight() {
        return this.bestBlockHeight;
    }

    public HexBytes getBestBlockHash() {
        return this.bestBlockHash;
    }

    public HexBytes getGenesisBlockHash() {
        return this.genesisBlockHash;
    }

    public long getPrunedHeight() {
        return this.prunedHeight;
    }

    public HexBytes getPrunedHash() {
        return this.prunedHash;
    }
}
