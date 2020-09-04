package org.tdf.sunflower.consensus;

import org.tdf.common.types.Uint256;

public interface EconomicModel {
    Uint256 getConsensusRewardAtHeight(long height);
}
