package org.tdf.common.store;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.tdf.common.serialize.Codec;

import java.util.Optional;
import java.util.function.BiFunction;

/**
 * delegate {@code Store<U, R>} as {@code Store<K, V>}
 *
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
    public void traverse(BiFunction<? super K, ? super V, Boolean> traverser) {
        store.traverse(
                (u, r) -> traverser.apply(keyCodec.getDecoder().apply(u), valueCodec.getDecoder().apply(r))
        );
    }
}