package org.tdf.common.store;

import lombok.NonNull;
import lombok.SneakyThrows;
import org.tdf.common.util.ByteArrayMap;

import java.util.*;

/**
 * make Map<K, V> implements Store<K,V> implicitly
 * @param <K> key type
 * @param <V> value type
 */
public class MapStore<K, V> implements Store<K, V> {
    private Map<K, V> map;

    protected Map<K, V> getMap(){
        return map;
    }

    private void assertKeyIsNotByteArray(K k){
        if((k instanceof byte[]) && !(map instanceof ByteArrayMap))
            throw new RuntimeException("please use ByteArrayMapStore instead of plain MapStore since byte array is mutable");
    }

    public MapStore() {
        this.map = new HashMap<>();
    }

    public MapStore(Map<K, V> map){
        this.map = map;
    }

    @Override
    public Optional<V> get(@NonNull K k) {
        assertKeyIsNotByteArray(k);
        return Optional.ofNullable(map.get(k));
    }

    @Override
    public void put(@NonNull K k, @NonNull V v) {
        assertKeyIsNotByteArray(k);
        map.put(k, v);
    }

    @Override
    public void putIfAbsent(@NonNull K k, @NonNull V v) {
        assertKeyIsNotByteArray(k);
        map.putIfAbsent(k, v);
    }

    @Override
    public void remove(@NonNull K k) {
        assertKeyIsNotByteArray(k);
        map.remove(k);
    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public boolean containsKey(@NonNull K k) {
        assertKeyIsNotByteArray(k);
        return map.containsKey(k);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public void flush() {
    }

    @Override
    @SneakyThrows
    public Map<K, V> asMap() {
        Map<K, V> ret = map.getClass().newInstance();
        ret.putAll(map);
        return ret;
    }
}
