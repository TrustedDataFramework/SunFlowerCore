package org.tdf.common.store;

import lombok.AllArgsConstructor;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

@AllArgsConstructor
public class StoreValuesView<V> implements Collection<V> {
    private Store<?, V> store;

    @Override
    public int size() {
        return store.size();
    }

    @Override
    public boolean isEmpty() {
        return store.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<V> iterator() {
        return store.stream()
                .map(Map.Entry::getValue)
                .iterator();
    }

    @Override
    public Object[] toArray() {
        return store.stream().map(Map.Entry::getValue).toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return (T[]) toArray();
    }

    @Override
    public boolean add(V v) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends V> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        store.clear();
    }
}
