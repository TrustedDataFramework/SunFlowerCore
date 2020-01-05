package org.tdf.common.store;

import org.tdf.common.util.ByteArrayMap;

import java.util.Map;

public class MemoryCachedStore<V> extends CachedStore<byte[], V> {
    public MemoryCachedStore(Store<byte[], V> delegated) {
        super(delegated);
    }

    @Override
    Map<byte[], V> newCache() {
        return new ByteArrayMap<>();
    }

    @Override
    Map<byte[], V> newDeleted() {
        return new ByteArrayMap<>();
    }

}
