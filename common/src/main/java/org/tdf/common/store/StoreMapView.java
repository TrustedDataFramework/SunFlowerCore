package org.tdf.common.store;

import lombok.AllArgsConstructor;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@AllArgsConstructor
public class StoreMapView<K, V> implements Map<K, V> {
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
    public boolean containsKey(Object key) {
        return store.containsKey((K) key);
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V get(Object key) {
        return store.get((K) key).orElse(null);
    }

    @Override
    public V put(K key, V value) {
        Optional<V> old = store.get(key);
        store.put(key, value);
        return old.orElse(null);
    }

    @Override
    public V remove(Object key) {
        Optional<V> old = store.get((K) key);
        store.remove((K) key);
        return old.orElse(null);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        m.forEach((k, v) -> {
            if (v == null) {
                store.remove(k);
                return;
            }
            store.put(k, v);
        });
    }

    @Override
    public void clear() {
        store.clear();
    }

    @Override
    public Set<K> keySet() {
        return new StoreKeySetView<>(store);
    }

    @Override
    public Collection<V> values() {
        return new StoreValuesView<>(store);
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return new StoreEntrySetView<>(store);
    }
}
