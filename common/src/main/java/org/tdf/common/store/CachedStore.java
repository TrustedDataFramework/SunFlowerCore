package org.tdf.common.store;

import lombok.NonNull;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Source which internally caches underlying Source key-value pairs
 * <p>
 *
 * @author zhuyingjie
 */
public class CachedStore<K, V> implements Store<K, V> {
    protected Store<K, V> delegate;

    protected Map<K, V> cache;

    protected V trap;

    protected Supplier<? extends Map<K, V>> cacheSupplier;

    @lombok.Builder(builderClassName = "Builder")
    public CachedStore(
            @NonNull Store<K, V> delegate,
            @NonNull Supplier<? extends Map<K, V>> cacheSupplier,
            @NonNull V trap
    ) {
        this.delegate = delegate;
        this.cacheSupplier = cacheSupplier;
        this.trap = trap;
        clearCache();
    }

    @Override
    public V getTrap() {
        return trap;
    }

    void clearCache() {
        cache = cacheSupplier.get();
    }

    @Override
    public Optional<V> get(@NonNull K k) {
        V v = cache.get(k);
        if (v == null) return delegate.get(k);
        if (v == getTrap()) return Optional.empty();
        return Optional.of(v);
    }

    @Override
    public void put(@NonNull K k, @NonNull V v) {
        cache.put(k, v);
    }

    @Override
    public void remove(@NonNull K k) {
        cache.put(k, getTrap());
    }

    /**
     * flush cache to underlying database
     */
    @Override
    public void flush() {
        if (cache.isEmpty()) return;
        if (delegate instanceof DatabaseStore) {
            DatabaseStore bat = (DatabaseStore) delegate;
            bat.putAll((Map<byte[], byte[]>) cache);
            bat.flush();
            clearCache();
            return;
        }
        cache.forEach((k, v) -> {
            if (v == getTrap()) {
                delegate.remove(k);
                return;
            }
            delegate.put(k, v);
        });
        clearCache();
        delegate.flush();
    }

    @Override
    public boolean containsKey(@NonNull K k) {
        V v = cache.get(k);
        return (v != null && v != getTrap())
                || (v != getTrap() && delegate.containsKey(k));
    }

    @Override
    public void clear() {
        clearCache();
        delegate.forEach((k, v) -> cache.put(k, getTrap()));
    }

    @Override
    public void traverse(BiFunction<? super K, ? super V, Boolean> traverser) {
        for (Map.Entry<K, V> entry : cache.entrySet()) {
            if (entry.getValue() == getTrap())
                continue;
            if (!traverser.apply(entry.getKey(), entry.getValue()))
                break;
        }
        delegate.traverse((k, v) -> {
            if (cache.containsKey(k))
                return true;
            return traverser.apply(k, v);
        });
    }
}
