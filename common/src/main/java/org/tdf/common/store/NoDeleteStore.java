package org.tdf.common.store;

import lombok.Getter;
import lombok.NonNull;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 * no delete store will store deleted key-value pair to @see deleted
 * when compact method called, clean the key-pari in @see deleted
 */
@Getter
public class NoDeleteStore<K, V> implements Store<K, V> {
    private Store<K, V> delegate;

    public NoDeleteStore(Store<K, V> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<V> get(@NonNull K k) {
        return delegate.get(k);
    }

    @Override
    public void put(@NonNull K k, @NonNull V v) {
        if(isTrap(v)) return;
        delegate.put(k, v);
    }

    @Override
    public void putIfAbsent(@NonNull K k, @NonNull V v) {
        if (delegate.containsKey(k)) return;
        put(k, v);
    }

    @Override
    public void remove(@NonNull K k) {
    }

    // flush all deleted to underlying db
    @Override
    public void flush() {
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
    public void clear(){
    }

    @Override
    public void traverse(BiFunction<? super K, ? super V, Boolean> traverser) {
        delegate.traverse(traverser);
    }

    @Override
    public Map<K, V> asMap() {
        return delegate.asMap();
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> consumer) {
        delegate.forEach(consumer);
    }

    @Override
    public Set<K> keySet() {
        return delegate.keySet();
    }

    @Override
    public Collection<V> values() {
        return delegate.values();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return delegate.entrySet();
    }

    @Override
    public Stream<Map.Entry<K, V>> stream() {
        return delegate.stream();
    }

    @Override
    public V getTrap() {
        return delegate.getTrap();
    }

    @Override
    public boolean isTrap(V v) {
        return delegate.isTrap(v);
    }
}
