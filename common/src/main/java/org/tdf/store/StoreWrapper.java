package org.tdf.store;

import org.tdf.common.Store;
import org.tdf.serialize.Deserializer;
import org.tdf.serialize.SerializeDeserializer;
import org.tdf.serialize.Serializer;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * wrap Store<byte[], byte[]> as Store<K, V>
 * @param <K> type of key
 * @param <V> type of value
 */
public class StoreWrapper<K, V>
        implements Store<K, V> {
    private Store<byte[], byte[]> store;

    private Serializer<? super K> keySerializer;
    private Deserializer<? extends K> keyDeserializer;
    private Serializer<? super V> valueSerializer;
    private Deserializer<? extends V> valueDeserializer;


    public StoreWrapper(
            Store<byte[], byte[]> store,
            SerializeDeserializer<K> keySD,
            SerializeDeserializer<V> valueSD
    ) {
        this.store = store;
        this.keySerializer = keySD;
        this.keyDeserializer = keySD;
        this.valueDeserializer = valueSD;
        this.valueSerializer = valueSD;
    }

    public StoreWrapper(Store<byte[], byte[]> store,
                        Serializer<? super K> keySerializer,
                        Deserializer<? extends K> keyDeserializer,
                        Serializer<? super V> valueSerializer,
                        Deserializer<? extends V> valueDeserializer
    ) {
        this.store = store;
        this.keySerializer = keySerializer;
        this.keyDeserializer = keyDeserializer;
        this.valueSerializer = valueSerializer;
        this.valueDeserializer = valueDeserializer;
    }

    private V createValueFromBytes(byte[] data) {
        return valueDeserializer.deserialize(data);
    }

    private K createKeyFromBytes(byte[] data) {
        return keyDeserializer.deserialize(data);
    }

    private byte[] getBytesFromKey(K k) {
        return keySerializer.serialize(k);
    }

    private byte[] getBytesFromValue(V v) {
        return valueSerializer.serialize(v);
    }

    @Override
    public Optional<V> get(K k) {
        return store.get(getBytesFromKey(k)).map(this::createValueFromBytes);
    }

    @Override
    public void put(K k, V v) {
        store.put(getBytesFromKey(k), getBytesFromValue(v));
    }

    @Override
    public void putIfAbsent(K k, V v) {
        store.putIfAbsent(getBytesFromKey(k), getBytesFromValue(v));
    }

    @Override
    public void remove(K k) {
        store.remove(getBytesFromKey(k));
    }

    @Override
    public Set<K> keySet() {
        return store.keySet().stream().map(this::createKeyFromBytes).collect(Collectors.toSet());
    }

    @Override
    public Collection<V> values() {
        return store.values().stream().map(this::createValueFromBytes).collect(Collectors.toList());
    }

    @Override
    public boolean containsKey(K k) {
        return store.containsKey(getBytesFromKey(k));
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
}