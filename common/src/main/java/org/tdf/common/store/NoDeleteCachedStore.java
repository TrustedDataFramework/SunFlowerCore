package org.tdf.common.store;

import lombok.NonNull;

import java.util.Map;
import java.util.function.Supplier;

public class NoDeleteCachedStore<K, V> extends CachedStore<K, V> {
    public NoDeleteCachedStore(@NonNull Store<K, V> delegate, @NonNull Supplier<? extends Map<K, V>> cacheSupplier
    ) {
        super(delegate, cacheSupplier);
    }

    @Override
    public void remove(Object o) {
    }
}
