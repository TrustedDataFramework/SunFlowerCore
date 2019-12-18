package org.tdf.sunflower.facade;

import org.tdf.common.types.Chained;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.ForkAbleState;

import java.util.Collection;
import java.util.Optional;

public interface StateTree<T extends ForkAbleState<T>> {
    void update(Block b);

    // provide all already updated state
    void put(Chained node, Collection<? extends T> allStates);

    Optional<T> get(String id, byte[] where);

    T getLastConfirmed(String id);

    void confirm(byte[] hash);

    // the state before this are thought immutable and cannot be fetched any more
    // used for state db persistent
    HexBytes getWhere();
}
