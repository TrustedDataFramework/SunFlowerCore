package org.tdf.sunflower.state;

import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * represents the state transition in block chain
 * @param <ID> id of the state
 * @param <S> type of state
 */
public interface StateUpdater<ID,S> {
    /**
     * get configured genesis states to initialize
     * @return genesis states
     */
    Map<ID, S> getGenesisStates();

    Set<ID> getRelatedKeys(Transaction transaction);

    Set<ID> getRelatedKeys(Collection<? extends Transaction> transactions);

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
