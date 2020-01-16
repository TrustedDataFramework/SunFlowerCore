package org.tdf.sunflower.facade;

import org.tdf.common.types.Chained;

import java.util.Collection;
import java.util.List;

// orphan node manager
public interface OrphansPool<T extends Chained> {
    List<T> filterAndCacheOrphans(Collection<? extends T> nodes);

    List<T> get(int page, int size);

    List<T> getAll();

    int size();

    List<T> getOrphanChainHeads();
}
