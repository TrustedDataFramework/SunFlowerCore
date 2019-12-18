package org.tdf.sunflower.facade;

import org.tdf.common.types.Chained;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.State;

import java.util.Optional;

public interface StateFactory<T extends State> {
    Optional<T> get(byte[] hash);
    void update(Block b);
    // provide already updated state
    void put(Chained where, T state);
    void confirm(byte[] hash);
    HexBytes getWhere();
    T getLastConfirmed();
}
