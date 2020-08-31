package org.tdf.sunflower.state;

import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Transaction;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractStateUpdater<ID, S> implements StateUpdater<ID, S> {
    public void update(Map<ID, S> beforeUpdate, Block block) {
        for (Transaction tx : block.getBody()) {
            update(beforeUpdate, block.getHeader(), tx);
        }
    }
}
