package org.tdf.sunflower.state;

import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.util.HashMap;
import java.util.Map;

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
     * update old state, generate a new state by the transition
     *
     * @param beforeUpdate state content
     * @param header       block header
     * @param transaction  transition function
     */
    void update(Map<ID, S> beforeUpdate, Header header, Transaction transaction);


    /**
     * batch update
     *
     * @param beforeUpdate key-value pair of states related
     * @param block        verified block
     */
    void update(Map<ID, S> beforeUpdate, Block block);

    default Map<ID, S> createEmptyMap() {
        return new HashMap<>();
    }
}
