package org.tdf.sunflower.facade;

import org.tdf.sunflower.types.Block;

import java.util.List;
import java.util.function.Function;

public interface ConfirmedBlocksProvider extends Function<List<Block>, List<Block>> {
}
