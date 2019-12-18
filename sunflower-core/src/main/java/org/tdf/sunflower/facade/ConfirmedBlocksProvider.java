package org.tdf.sunflower.facade;

import org.tdf.sunflower.types.Block;

import java.util.List;

public interface ConfirmedBlocksProvider {
    List<Block> getConfirmed(List<Block> unconfirmed);
}
