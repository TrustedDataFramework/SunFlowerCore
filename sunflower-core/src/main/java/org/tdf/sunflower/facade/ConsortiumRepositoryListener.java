package org.tdf.sunflower.facade;

import org.tdf.sunflower.types.Block;

public interface ConsortiumRepositoryListener{
    void onBlockWritten(Block block);
    void onNewBestBlock(Block block);
    void onBlockConfirmed(Block block);
}
