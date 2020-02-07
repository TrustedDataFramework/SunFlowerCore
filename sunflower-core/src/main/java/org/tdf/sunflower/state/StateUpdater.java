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
 *
 * @param <ID> id of the state
 * @param <S>  type of state
 */
public interface StateUpdater<ID, S> {
    /**
     * get configured genesis states to initialize
     *
     * @return genesis states
     */
    Map<ID, S> getGenesisStates();

    /**
     * get all related state related to this transaction
     * @param transaction the state transition function
     * @return all related state related to transition
     */
    Set<ID> getRelatedKeys(Transaction transaction);

    /**
     * get related state of transition collections
     * @param transactions collection of transition
     * @return related states
     */
    Set<ID> getRelatedKeys(Collection<? extends Transaction> transactions);

    /**
     * get related state of a block
     * @param block block is a collection of verified transitions
     * @return related states
     */
    Set<ID> getRelatedKeys(Block block);

    /**
     * get related state of collection of blocks
     * @param block list of block
     * @return related states
     */
    Set<ID> getRelatedKeys(List<Block> block);

    /**
     * update old state, generate a new state by the transition
     * @param id key of state
     * @param state state content
     * @param header block header
     * @param transaction transition function
     * @return new state
     */
    S update(ID id, S state, Header header, Transaction transaction);

    /**
     * generate an empty state
     * @param id key of state
     * @return a empty state
     */
    S createEmpty(ID id);

    /**
     * batch update
     * @param beforeUpdate key-value pair of states related
     * @param block verified block
     * @return new state updated
     */
    Map<ID, S> update(Map<ID, S> beforeUpdate, Block block);

    /**
     * batch update
     * @param beforeUpdate key-value pair of states related
     * @param blocks verified blocks
     * @return updated states
     */
    Map<ID, S> update(Map<ID, S> beforeUpdate, List<Block> blocks);

    /**
     * create an empty map to store key-value pair
     * @return an empty map
     */
    Map<ID, S> createEmptyMap();

    /**
     * create an empty set to store key-value pair
     * @return an empty set
     */
    Set<ID> createEmptySet();
}
