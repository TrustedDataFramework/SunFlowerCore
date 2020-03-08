package org.tdf.sunflower.consensus;

public interface MinerConfig {
    int getMaxBodySize();

    boolean isAllowEmptyBlock();
}
