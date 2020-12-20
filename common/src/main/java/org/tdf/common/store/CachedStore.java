package org.tdf.common.store;

import lombok.NonNull;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Source which internally caches modification to underlying store
 * <p>
 *
 * @author zhuyingjie
 */
public class CachedStore<K, V> implements Store<K, V> {
    protected Store<K, V> delegate;

    protected Map<K, V> cache;

    protected Supplier<? extends Map<K, V>> cacheSupplier;

    @lombok.Builder(builderClassName = "Builder")
    public CachedStore(
            @NonNull Store<K, V> delegate,
            @NonNull Supplier<? extends Map<K, V>> cacheSupplier
    ) {
        this.delegate = delegate;
        this.cacheSupplier = cacheSupplier;
        clearCache();
    }

    @Override
    public V getTrap() {
        return delegate.getTrap();
    }

    @Override
    public boolean isTrap(V v) {
        return delegate.isTrap(v);
    }

    void clearCache() {
        cache = cacheSupplier.get();
    }

    @Override
    public Optional<V> get(@NonNull K k) {
        V v = cache.get(k);
        if (v == null) return delegate.get(k);
        if (isTrap(v)) return Optional.empty();
        return Optional.of(v);
    }

    @Override
    public void put(@NonNull K k, @NonNull V v) {
        cache.put(k, v);
    }

    /**
     * when remove key, mark the key is removed in
     *
     * @param k key of key-value mapping
     */
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
        if (delegate instanceof BatchStore) {
            BatchStore<K, V> bat = (BatchStore<K, V>) delegate;
            bat.putAll(cache.entrySet());
            bat.flush();
            clearCache();
            return;
        }
        cache.forEach(delegate::put);
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
            if (isTrap(entry.getValue()))
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
