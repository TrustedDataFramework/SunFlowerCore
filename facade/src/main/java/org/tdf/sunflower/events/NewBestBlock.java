package org.tdf.sunflower.events;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.tdf.sunflower.types.Block;

@Value
@AllArgsConstructor
public class NewBestBlock {
   Block block;
}
