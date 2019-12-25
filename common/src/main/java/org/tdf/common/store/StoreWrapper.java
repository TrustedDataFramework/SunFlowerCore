package org.tdf.common.store;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.tdf.common.serialize.Codec;

import java.util.*;
import java.util.stream.Collectors;

/**
 * wrap Store<byte[], byte[]> as Store<K, V>
 * @param <K> type of key
 * @param <V> type of value
 */
@AllArgsConstructor
public class StoreWrapper<K, V, U, R>
        implements Store<K, V> {
    private Store<U, R> store;

    private Codec<K, U> keyCodec;
    private Codec<V, R> valueCodec;


    @Override
    public Optional<V> get(@NonNull K k) {
        return store.get(keyCodec.getEncoder().apply(k)).map(valueCodec.getDecoder());
    }

    @Override
    public void put(@NonNull K k, @NonNull V v) {
        store.put(keyCodec.getEncoder().apply(k), valueCodec.getEncoder().apply(v));
    }

    @Override
    public void putIfAbsent(@NonNull K k, @NonNull V v) {
        store.putIfAbsent(keyCodec.getEncoder().apply(k), valueCodec.getEncoder().apply(v));
    }

    @Override
    public void remove(@NonNull K k) {
        store.remove(keyCodec.getEncoder().apply(k));
    }

    @Override
    public Set<K> keySet() {
        return store.keySet().stream()
                .map(keyCodec.getDecoder())
                .collect(Collectors.toSet());
    }

    @Override
    public Collection<V> values() {
        return store.values().stream()
                .map(valueCodec.getDecoder())
                .collect(Collectors.toList());
    }

    @Override
    public boolean containsKey(@NonNull K k) {
        return store.containsKey(keyCodec.getEncoder().apply(k));
    }

    @Override
    public int size() {
        return store.size();
    }

    @Override
    public boolean isEmpty() {
        return store.isEmpty();
    }

    @Override
    public void clear() {
        store.clear();
    }

    @Override
    public void flush() {

    }

    @Override
    public Map<K, V> asMap() {
        Map<K, V> m = new HashMap<>();
        for(Map.Entry<U, R> entry: store.asMap().entrySet()){
            m.put(keyCodec.getDecoder().apply(entry.getKey()), valueCodec.getDecoder().apply(entry.getValue()));
        }
        return m;
    }
}