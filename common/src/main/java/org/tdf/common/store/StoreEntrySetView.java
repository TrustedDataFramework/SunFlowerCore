package org.tdf.common.store;

import lombok.AllArgsConstructor;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@AllArgsConstructor
public class StoreEntrySetView<K, V> implements Set<Map.Entry<K, V>> {
    private Store<K, V> store;

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
        return store.containsKey(((Map.Entry<K, V>) o).getKey());
    }

    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
        return new StoreIterator<>(store.stream().iterator(), this::remove);
    }

    @Override
    public Object[] toArray() {
        return store.stream().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return (T[]) toArray();
    }

    @Override
    public boolean add(Map.Entry<K, V> kvEntry) {
        V old = store.get(kvEntry.getKey()).orElse(null);
        store.put(kvEntry.getKey(), kvEntry.getValue());
        return old == kvEntry.getValue();
    }

    @Override
    public boolean remove(Object o) {
        boolean ret = contains(o);
        store.remove(((Map.Entry<K, V>) o).getKey());
        return ret;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            K k = ((Map.Entry<K, V>) o).getKey();
            if (!store.containsKey(k)) return false;
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends Map.Entry<K, V>> c) {
        boolean ret = false;
        for (Map.Entry<K, V> entry : c) {
            ret |= add(entry);
        }
        return ret;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException(); // TODO add later if required
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
