package org.tdf.sunflower.state;

import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Transaction;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractStateUpdater<ID, S> implements StateUpdater<ID, S> {
    @Override
    public Set<ID> getRelatedKeys(Collection<? extends Transaction> transactions) {
        Set<ID> set = createEmptySet();
        transactions.forEach(t -> set.addAll(getRelatedKeys(t)));
        return set;
    }

    @Override
    public Set<ID> getRelatedKeys(Block block) {
        return getRelatedKeys(block.getBody());
    }

    @Override
    public Set<ID> getRelatedKeys(List<Block> block) {
        Set<ID> set = createEmptySet();
        block.forEach(b -> set.addAll(getRelatedKeys(b)));
        return set;
    }

    public Map<ID, S> update(Map<ID, S> beforeUpdate, Block block) {
        Map<ID, S> ret = createEmptyMap();
        ret.putAll(beforeUpdate);
        block.getBody().forEach(
                tx -> getRelatedKeys(tx)
                        .forEach(k -> ret.put(k, update(k, ret.get(k), block.getHeader(), tx)))
        );
        return ret;
    }

    public Map<ID, S> update(Map<ID, S> beforeUpdate, List<Block> blocks) {
        Map<ID, S> ret = createEmptyMap();
        ret.putAll(beforeUpdate);
        blocks.forEach(b -> {
            b.getBody().forEach(tx -> {
                getRelatedKeys(tx).forEach(k -> {
                    ret.put(k, update(k, ret.get(k), b.getHeader(), tx));
                });
            });
        });
        return ret;
    }
}
