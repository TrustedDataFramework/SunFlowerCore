package org.tdf.common.store;

import lombok.NonNull;
import org.tdf.common.util.ByteArrayMap;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Source which internally caches underlying Source key-value pairs
 * <p>
 * Created by Anton Nashatyrev on 21.10.2016.
 */
public abstract class CachedStore<K, V> implements Store<K, V> {
    protected Store<K, V> delegated;

    protected Map<K, V> cache;

    protected Map<K, V> deleted;

    public CachedStore(Store<K, V> delegated) {
        this.delegated = delegated;
        clearCache();
    }

    // create a new cache
    abstract Map<K, V> newCache();

    // get non-null static trap value
    // put key with trap value will remove the key
    abstract V getTrap();

    void clearCache() {
        cache = newCache();
    }

    @Override
    public Optional<V> get(@NonNull K k) {
        V v = cache.get(k);
        if (v == null) return delegate.get(k);
        if (v == getTrap()) return Optional.empty();
        return v;
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
        if (delegated instanceof DatabaseStore) {
            DatabaseStore bat = (DatabaseStore) delegated;
            bat.putAll(cache);
            bat.flush();
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
        delegated.flush();
    }

    @Override
    public boolean containsKey(@NonNull K k) {
        V v = cache.get(k);
        return (v != null && v != getTrap())
         || (v != getTrap() && delegate.containsKey(k));
    }


    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public void clear() {
        clearCache();
        delegated.forEach((k, v) -> cache.put(k, getTrap()));
    }

    @Override
    public void forEach(BiConsumer<K, V> consumer) {
        cache.forEach((k, v) -> {if(v != getTrap()) consumer.accept(k, v);});
        delegated.forEach((k, v) -> {
            if (cache.containsKey(k)) return;
            consumer.accept(k, v);
        });
    }
}
