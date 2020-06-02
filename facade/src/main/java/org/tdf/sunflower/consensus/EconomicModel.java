package org.tdf.sunflower.consensus;

public interface EconomicModel {
    long getConsensusRewardAtHeight(long height);
}
