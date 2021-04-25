package org.tdf.common.store;

import lombok.NonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * delegate Map as Store
 *
 * @param <K> key type
 * @param <V> value type
 */
public class MapStore<K, V> implements BatchStore<K, V> {
    private Map<K, V> map;

    public MapStore() {
        this.map = new HashMap<>();
    }

    public MapStore(Map<K, V> map) {
        this.map = map;
    }

    protected Map<K, V> getMap() {
        return map;
    }

    @Override
    public V get(@NonNull K k) {
        return map.get(k);
    }

    @Override
    public void put(@NonNull K k, @NonNull V v) {
        map.put(k, v);
    }

    @Override
    public void putAll(Collection<? extends Map.Entry<? extends K, ? extends V>> rows) {
        rows.forEach(entry -> {
            if (entry.getValue() == null) {
                map.remove(entry.getKey());
                return;
            }
            map.put(entry.getKey(), entry.getValue());
        });
    }

    @Override
    public void remove(@NonNull K k) {
        map.remove(k);
    }

    @Override
    public void flush() {
    }
}
