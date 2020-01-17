package org.tdf.sunflower.facade;

import org.tdf.sunflower.types.Block;

@Deprecated
// subscribe to event bus instead
/**
 * @see org.tdf.common.event.EventBus
 * @see org.tdf.sunflower.events.NewBlockWritten
 * @see org.tdf.sunflower.events.NewBestBlock
 * @see org.tdf.sunflower.events.NewBlockConfirmed
 */
public interface ConsortiumRepositoryListener{
    void onBlockWritten(Block block);
    void onNewBestBlock(Block block);
    void onBlockConfirmed(Block block);
}
