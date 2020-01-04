package org.tdf.sunflower.state;

import org.tdf.sunflower.types.Block;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractStateUpdater<ID, S> implements StateUpdater<ID, S>{
    @Override
    public Set<ID> getRelatedKeys(Block block) {
        Set<ID> set = createEmptySet();
        block.getBody().forEach(tx -> set.addAll(this.getRelatedKeys(tx)));
        return set;
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
                        .forEach(k -> ret.put(k, update(k, ret.get(k), tx)))
        );
        return ret;
    }

    public Map<ID, S> update(Map<ID, S> beforeUpdate, List<Block> blocks) {
        Map<ID, S> ret = createEmptyMap();
        ret.putAll(beforeUpdate);
        blocks.stream().flatMap(b -> b.getBody().stream())
                .forEach(tx -> getRelatedKeys(tx).forEach(k -> {
                    ret.put(k, update(k, ret.get(k), tx));
                }));
        return ret;
    }
}
