package org.tdf.sunflower.facade;

import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.ValidateResult;

/**
 * validate blocks
 */
public interface BlockValidator {

    /**
     * validate the block
     *
     * @param block      the block to validate
     * @param dependency parent block
     * @return validate result
     */
    ValidateResult validate(Block block, Block dependency);
}
