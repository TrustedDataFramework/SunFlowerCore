package org.tdf.common.store;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * no delete store will store deleted key-value pair to @see deleted
 * when compact method called, clean the key-pari in @see deleted
 */
@AllArgsConstructor
@Getter(AccessLevel.PROTECTED)
public class NoDeleteStore<K, V> implements Store<K, V> {
    private Store<K, V> delegate;

    private Store<K, V> removed;

    @Override
    public Optional<V> get(@NonNull K k) {
        return delegate.get(k);
    }

    @Override
    public void put(@NonNull K k, @NonNull V v) {
        removed.remove(k);
        delegate.put(k, v);
    }

    @Override
    public void putIfAbsent(@NonNull K k, @NonNull V v) {
        if (delegate.containsKey(k)) return;
        put(k, v);
    }

    @Override
    public void remove(@NonNull K k) {
        Optional<V> o = get(k);
        if (!o.isPresent()) return;
        // we not remove k, just add it to a cache
        // when flush() called, we remove k in the cache
        removed.put(k, o.get());
    }

    // flush all deleted to underlying db
    @Override
    public void flush() {
        removed.flush();
        delegate.flush();
    }

    @Override
    public boolean containsKey(@NonNull K k) {
        return delegate.containsKey(k);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public void clear() {
        removed = delegate;
    }

    @Override
    public void forEach(BiConsumer<K, V> consumer) {
        delegate.forEach(consumer);
    }

    public void compact() {
        if (removed == delegate) {
            delegate.clear();
            return;
        }
        removed.forEach((k, v) -> delegate.remove(k));
        removed.clear();
    }

    public void compact(Set<K> excludes) {
        removed.stream()
                .map(Map.Entry::getKey)
                .filter(k -> !excludes.contains(k))
                .collect(Collectors.toList())
                .forEach(k -> {
                    removed.remove(k);
                    delegate.remove(k);
                });
    }

    @Override
    public Map<K, V> asMap() {
        return delegate.asMap();
    }

}
