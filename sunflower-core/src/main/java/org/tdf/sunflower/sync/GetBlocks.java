package org.tdf.sunflower.sync;

public class GetBlocks {
    private long startHeight;
    private long stopHeight;
    private boolean descend;
    private int limit;

    public GetBlocks(long startHeight, long stopHeight, boolean descend, int limit) {
        this.startHeight = startHeight;
        this.stopHeight = stopHeight;
        this.descend = descend;
        this.limit = limit;
    }

    public GetBlocks() {
    }

    GetBlocks clip() {
        if (stopHeight - startHeight < limit) return this;
        if (descend) {
            startHeight = stopHeight - limit;
        } else {
            stopHeight = startHeight + limit;
        }
        return this;
    }

    public long getStartHeight() {
        return this.startHeight;
    }

    public long getStopHeight() {
        return this.stopHeight;
    }

    public boolean isDescend() {
        return this.descend;
    }

    public int getLimit() {
        return this.limit;
    }
}
