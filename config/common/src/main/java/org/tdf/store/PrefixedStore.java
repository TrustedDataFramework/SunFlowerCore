package org.tdf.store;

import com.google.common.primitives.Bytes;
import org.tdf.common.Store;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * wrap store as a prefixed
 * @param <V>
 */
public class PrefixedStore<V> implements Store<byte[], V> {
    private static final String ERROR = "low performance op";

    private void hint(){
        new Exception().printStackTrace();
        System.err.println(ERROR);
    }

    private byte[] prefix;
    private Store<byte[], V> store;

    public PrefixedStore(String prefix, Store<byte[], V> store) {
        this.prefix = prefix.getBytes(StandardCharsets.UTF_8);
        this.store = store;
    }

    @Override
    public Optional<V> get(byte[] bytes) {
        return store.get(Bytes.concat(prefix, bytes));
    }

    @Override
    public void put(byte[] bytes, V v) {
        store.put(Bytes.concat(prefix, bytes), v);
    }

    @Override
    public void putIfAbsent(byte[] bytes, V v) {
        store.putIfAbsent(Bytes.concat(prefix, bytes), v);
    }

    @Override
    public void remove(byte[] bytes) {
        store.remove(Bytes.concat(prefix, bytes));
    }

    @Override
    public Set<byte[]> keySet() {
        return store.keySet().stream()
                .filter(x -> Arrays.equals(prefix, Arrays.copyOfRange(x, 0, prefix.length)))
                .map(x -> Arrays.copyOfRange(x, prefix.length, x.length))
                .collect(Collectors.toSet());
    }

    @Override
    public Collection<V> values() {
        return keySet().stream().map(x -> get(x).orElse(null))
                .filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public boolean containsKey(byte[] bytes) {
        return store.containsKey(Bytes.concat(prefix, bytes));
    }

    @Override
    public int size() {
        return keySet().size();
    }

    @Override
    public boolean isEmpty() {
        return keySet().size() == 0;
    }

    @Override
    public void clear() {
        keySet().forEach(this::remove);
    }
}