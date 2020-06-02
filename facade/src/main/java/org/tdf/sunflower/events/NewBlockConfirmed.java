package org.tdf.sunflower.events;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.tdf.sunflower.types.Block;

@Value
@AllArgsConstructor
public class NewBlockConfirmed {
    private Block block;
}
