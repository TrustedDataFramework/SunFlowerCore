package org.tdf.common;

import org.tdf.exception.StateUpdateException;

public interface State<T> extends Cloneable<T> {
    void update(Block b, Transaction t) throws StateUpdateException;

    // some state are per-block, not per-transaction
    void update(Header header) throws StateUpdateException;
}
