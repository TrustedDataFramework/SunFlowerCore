package org.tdf.sunflower.consensus.poa

import org.tdf.common.types.Uint256
import org.tdf.sunflower.consensus.EconomicModel

class PoAModel : EconomicModel {
    override fun rewardAt(height: Long): Uint256 {
        val era = height / HALF_PERIOD
        var reward = INITIAL_SUPPLY
        for (i in 0 until era) {
            reward = reward * 52218182 / 100000000
        }
        return Uint256.of(reward)
    }

    companion object {
        private const val INITIAL_SUPPLY: Long = 20
        private const val HALF_PERIOD: Long = 10000000
    }
}