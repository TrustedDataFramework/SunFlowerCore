package org.tdf.common.store;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.tdf.common.serialize.Codec;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * delegate {@code Store<U, R>} as {@code Store<K, V>}
 *
 * @param <K> type of key
 * @param <V> type of value
 */
@AllArgsConstructor
public class StoreWrapper<K, V, U, R>
        implements BatchStore<K, V> {

    @Getter
    private Store<byte[], byte[]> store;

    private Codec<K> keyCodec;
    private Codec<V> valueCodec;


    @Override
    public V get(@NonNull K k) {
        byte[] v = store.get(keyCodec.getEncoder().apply(k));
        if (v == null || v.length == 0)
            return null;
        return valueCodec.getDecoder().apply(v);
    }

    @Override
    public void put(@NonNull K k, @NonNull V v) {
        store.put(keyCodec.getEncoder().apply(k), valueCodec.getEncoder().apply(v));
    }


    @Override
    public void remove(@NonNull K k) {
        store.remove(keyCodec.getEncoder().apply(k));
    }

    @Override
    public void flush() {

    }


    @Override
    public void putAll(Collection<? extends Map.Entry<? extends K, ? extends V>> rows) {
        ((BatchStore<byte[], byte[]>) store).putAll(
                rows.stream().map(e -> new AbstractMap.SimpleEntry<>(
                        keyCodec.getEncoder().apply(e.getKey()),
                        valueCodec.getEncoder().apply(e.getValue())
                ))
                        .collect(Collectors.toList())
        );
    }
}