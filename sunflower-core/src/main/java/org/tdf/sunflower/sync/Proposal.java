package org.tdf.sunflower.sync;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tdf.sunflower.types.Block;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Proposal {
    private Block block;
}
