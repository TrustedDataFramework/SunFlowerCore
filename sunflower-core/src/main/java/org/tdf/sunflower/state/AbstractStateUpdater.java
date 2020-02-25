package org.tdf.sunflower.state;

import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Transaction;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractStateUpdater<ID, S> implements StateUpdater<ID, S> {
    @Override
    public Set<ID> getRelatedKeys(Collection<? extends Transaction> transactions, Map<ID, S> store) {
        Set<ID> set = createEmptySet();
        transactions.forEach(t -> set.addAll(getRelatedKeys(t, store)));
        return set;
    }

    @Override
    public Set<ID> getRelatedKeys(Block block, Map<ID, S> store) {
        return getRelatedKeys(block.getBody(), store);
    }


    public Map<ID, S> update(Map<ID, S> beforeUpdate, Block block) {
        Map<ID, S> ret = createEmptyMap();
        ret.putAll(beforeUpdate);
        for (Transaction tx : block.getBody()) {
            Map<ID, S> tmp = createEmptyMap();
            getRelatedKeys(tx, ret)
                    .forEach(k -> tmp.put(k, ret.get(k)));
            Map<ID, S> tmp2 = update(tmp, block.getHeader(), tx);
            ret.putAll(tmp2);
        }
        return ret;
    }
}
