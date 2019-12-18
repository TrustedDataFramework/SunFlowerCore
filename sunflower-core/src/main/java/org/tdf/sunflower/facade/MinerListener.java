package org.tdf.sunflower.facade;

import org.tdf.sunflower.types.Block;

public interface MinerListener {

    void onBlockMined(Block block);

    void onMiningFailed(Block block);
}
