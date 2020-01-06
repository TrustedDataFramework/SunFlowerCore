package org.tdf.common.store;

import lombok.NonNull;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class ReadOnlyStore<K, V> implements Store<K, V> {
    private static final String READ_ONLY_TIP = "the store is read only";

    private Store<K, V> delegate;

    public ReadOnlyStore(Store<K, V> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<V> get(@NonNull K k) {
        return delegate.get(k);
    }

    @Override
    public void put(@NonNull K k, @NonNull V v) {
        throw new UnsupportedOperationException(READ_ONLY_TIP);
    }

    @Override
    public void putIfAbsent(@NonNull K k, @NonNull V v) {
        throw new UnsupportedOperationException(READ_ONLY_TIP);
    }

    @Override
    public void remove(@NonNull K k) {
        throw new UnsupportedOperationException(READ_ONLY_TIP);
    }

    @Override
    public void flush() {
        throw new UnsupportedOperationException(READ_ONLY_TIP);
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
        throw new UnsupportedOperationException(READ_ONLY_TIP);
    }

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException(READ_ONLY_TIP);
    }

    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException(READ_ONLY_TIP);
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException(READ_ONLY_TIP);
    }


    @Override
    public Map<K, V> asMap() {
        throw new UnsupportedOperationException(READ_ONLY_TIP);
    }


    @Override
    public void traverse(BiFunction<? super K, ? super V, Boolean> traverser) {
        delegate.traverse(traverser);
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> consumer) {
        delegate.forEach(consumer);
    }

    @Override
    public V getTrap() {
        return delegate.getTrap();
    }

    @Override
    public Stream<Map.Entry<K, V>> stream() {
        return delegate.stream();
    }
}
