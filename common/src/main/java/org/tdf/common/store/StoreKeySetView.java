package org.tdf.common.store;

import lombok.AllArgsConstructor;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@AllArgsConstructor
public class StoreKeySetView<K> implements Set<K> {
    private Store<K, ?> store;

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
        return store.containsKey((K) o);
    }

    @Override
    public Iterator<K> iterator() {
        return new StoreIterator<>(store.stream()
                .map(Map.Entry::getKey)
                .iterator(), this::remove);
    }

    @Override
    public Object[] toArray() {
        return store.stream().map(Map.Entry::getKey)
                .toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return (T[]) toArray();
    }

    @Override
    public boolean add(K k) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        boolean ret = store.containsKey((K) o);
        store.remove((K) o);
        return ret;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            K k = (K) o;
            if (!store.containsKey(k)) return false;
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends K> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean ret = false;
        for (Object o : c) {
            ret |= remove(o);
        }
        return ret;
    }

    @Override
    public void clear() {
        store.clear();
    }
}
