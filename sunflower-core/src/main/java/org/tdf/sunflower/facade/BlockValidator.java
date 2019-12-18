package org.tdf.sunflower.facade;

import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.ValidateResult;

public interface BlockValidator {
    ValidateResult validate(Block block, Block dependency);
}
