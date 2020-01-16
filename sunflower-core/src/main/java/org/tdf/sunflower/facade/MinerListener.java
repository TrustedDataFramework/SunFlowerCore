package org.tdf.sunflower.facade;

import org.tdf.sunflower.types.Block;

@Deprecated
// subscribe to event bus instead
/**
 * @see org.tdf.common.event.EventBus
 */
public interface MinerListener {

    void onBlockMined(Block block);

    void onMiningFailed(Block block);
}
