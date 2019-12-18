package org.tdf.common.store;

import lombok.AllArgsConstructor;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@AllArgsConstructor
public class NoDoubleDeleteStore<K, V> implements Store<K, V>{
    private Store<K, V> delegate;

    @Override
    public Optional<V> get(K k) {
        return delegate.get(k);
    }

    @Override
    public void put(K k, V v) {
        delegate.put(k, v);
    }

    @Override
    public void putIfAbsent(K k, V v) {
        delegate.putIfAbsent(k, v);
    }

    @Override
    public void remove(K k) {
        if(!containsKey(k)) throw new RuntimeException("trying to delete a non-exists key");
        delegate.remove(k);
    }

    @Override
    public void flush() {
        delegate.flush();
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
    public boolean containsKey(K k) {
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
        delegate.clear();
    }
}
