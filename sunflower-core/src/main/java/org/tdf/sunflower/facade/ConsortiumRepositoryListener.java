package org.tdf.sunflower.facade;

import org.tdf.sunflower.types.Block;

@Deprecated
// subscribe to event bus instead
/**
 * @see org.tdf.common.event.EventBus
 */
public interface ConsortiumRepositoryListener{
    void onBlockWritten(Block block);
    void onNewBestBlock(Block block);
    void onBlockConfirmed(Block block);
}
