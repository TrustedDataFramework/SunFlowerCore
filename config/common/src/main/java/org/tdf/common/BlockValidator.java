package org.tdf.common;

public interface BlockValidator {
    ValidateResult validate(Block block, Block dependency);
}
