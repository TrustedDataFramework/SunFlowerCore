package org.tdf.common;

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
