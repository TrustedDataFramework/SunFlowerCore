package org.tdf.common.store;

import lombok.NonNull;
import org.tdf.common.trie.ReadOnlyTrie;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class ReadOnlyStore<K, V> implements Store<K, V> {
    private static final String READ_ONLY_TIP = "the store is read only";

    private Store<K, V> delegate;

    private ReadOnlyStore(Store<K, V> delegate) {
        this.delegate = delegate;
    }

    public static <K, V> Store<K, V> of(Store<K, V> delegate) {
        if (delegate instanceof ReadOnlyStore || delegate instanceof ReadOnlyTrie)
            return delegate;
        return new ReadOnlyStore<>(delegate);
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
        return Collections.unmodifiableSet(delegate.keySet());
    }

    @Override
    public Collection<V> values() {
        return Collections.unmodifiableCollection(delegate.values());
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return Collections.unmodifiableSet(delegate.entrySet());
    }


    @Override
    public Map<K, V> asMap() {
        return Collections.unmodifiableMap(delegate.asMap());
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

    @Override
    public boolean isTrap(V v) {
        return delegate.isTrap(v);
    }
}
