package org.tdf.sunflower.consensus.poa;

import org.tdf.common.types.Uint256;
import org.tdf.sunflower.consensus.EconomicModel;

public class PoAEconomicModel implements EconomicModel {

    private static final long INITIAL_SUPPLY = 20;

    private static final long HALF_PERIOD = 10000000;


    public Uint256 getConsensusRewardAtHeight(long height) {
        long era = height / HALF_PERIOD;
        long reward = INITIAL_SUPPLY;
        for (long i = 0; i < era; i++) {
            reward = reward * 52218182 / 100000000;
        }
        return Uint256.of(reward);
    }

}
