package org.tdf.sunflower.facade;

import org.tdf.sunflower.types.Block;

@Deprecated
// subscribe to event bus instead
/**
 * @see org.tdf.common.event.EventBus
 * @see org.tdf.sunflower.events.NewBlockMined
 * @see org.tdf.sunflower.events.MiningFailed
 */
public interface MinerListener {

    void onBlockMined(Block block);

    void onMiningFailed(Block block);
}
