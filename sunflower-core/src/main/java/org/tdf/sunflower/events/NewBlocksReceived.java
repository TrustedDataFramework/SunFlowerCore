package org.tdf.sunflower.events;

import lombok.Value;
import org.tdf.sunflower.types.Block;

import java.util.List;

@Value
public class NewBlocksReceived {
    private List<Block> blocks;
}
