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

    abstract Map<K, V> newCache();

    abstract Map<K, V> newDeleted();

    void clearCache() {
        cache = newCache();
        deleted = newDeleted();
    }

    @Override
    public Optional<V> get(@NonNull K k) {
        if (deleted.containsKey(k)) return Optional.empty();
        V v = cache.get(k);
        if (v != null) return Optional.of(v);
        return delegated.get(k);
    }

    @Override
    public void put(@NonNull K k, @NonNull V v) {
        deleted.remove(k);
        cache.put(k, v);
    }

    @Override
    public void remove(@NonNull K k) {
        cache.remove(k);
        Optional<V> v = delegated.get(k);
        if (!v.isPresent()) return;
        deleted.put(k, v.get());
    }

    /**
     * flush cache to underlying database
     */
    @Override
    public void flush() {
        if (cache.isEmpty() && deleted.isEmpty()) return;
        if (delegated instanceof DatabaseStore) {
            DatabaseStore bat = (DatabaseStore) delegated;
            Map<byte[], byte[]> modifies = new ByteArrayMap<>((Map<byte[], byte[]>) cache);
            deleted.forEach((k, v) -> modifies.put((byte[]) k, DatabaseStore.EMPTY));
            bat.putAll(modifies);
            bat.flush();
            return;
        }
        deleted.forEach((k, v) -> delegated.remove(k));
        cache.forEach((k, v) -> delegated.put(k, v));
        clearCache();
        delegated.flush();
    }

    @Override
    public boolean containsKey(@NonNull K k) {
        return !deleted.containsKey(k) && (cache.containsKey(k) || delegated.containsKey(k));
    }


    @Override
    public boolean isEmpty() {
        return cache.isEmpty() && deleted.size() == delegated.size();
    }

    @Override
    public void clear() {
        clearCache();
        delegated.forEach(deleted::put);
    }

    @Override
    public void forEach(BiConsumer<K, V> consumer) {
        cache.forEach(consumer);
        delegated.forEach((k, v) -> {
            if (deleted.containsKey(k) || cache.containsKey(k)) return;
            consumer.accept(k, v);
        });
    }
}
