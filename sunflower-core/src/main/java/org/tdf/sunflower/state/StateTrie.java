package org.tdf.sunflower.state;

import org.tdf.sunflower.types.Block;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface StateTrie<ID, S> {
    // get an optional state at a block
    Optional<S> get(byte[] rootHash, ID id);
    Map<ID, S> batchGet(byte[] rootHash, Collection<ID> keys);

    // commit a new block
    void commit(Block block);

    // commit blocks
    default void commit(List<Block> blocks){
        blocks.forEach(this::commit);
    }
}
