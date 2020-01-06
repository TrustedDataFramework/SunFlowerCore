package org.tdf.sunflower.state;

import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface StateUpdater<ID,S> {
    Map<ID, S> getGenesisStates();

    Set<ID> getRelatedKeys(Transaction transaction);

    Set<ID> getRelatedKeys(Block block);

    Set<ID> getRelatedKeys(List<Block> block);

    // the update method should always returns a new state
    S update(ID id, S state, Header header, Transaction transaction);

    S createEmpty(ID id);

    Map<ID, S> update(Map<ID, S> beforeUpdate, Block block);

    Map<ID, S> update(Map<ID, S> beforeUpdate, List<Block> blocks);

    Map<ID, S> createEmptyMap();

    Set<ID> createEmptySet();
}
