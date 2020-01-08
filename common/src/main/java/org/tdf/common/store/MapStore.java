package org.tdf.common.store;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.tdf.common.util.ByteArrayMap;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Stream;

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

    private void assertKeyIsNotByteArray(K k) {
        if ((k instanceof byte[]) && !(map instanceof ByteArrayMap))
            throw new RuntimeException("please use ByteArrayMapStore instead of plain MapStore since byte array is mutable");
    }

    @Override
    public Optional<V> get(@NonNull K k) {
        return Optional.ofNullable(map.get(k));
    }

    @Override
    public void put(@NonNull K k, @NonNull V v) {
        if (isTrap(v)) {
            map.remove(k);
            return;
        }
        map.put(k, v);
    }

    @Override
    public void putAll(@NonNull Map<K, V> rows) {
        rows.forEach((k, v) -> {
            if (v == null || isTrap(v)) {
                map.remove(k);
                return;
            }
            map.put(k, v);
        });
    }

    @Override
    public void putIfAbsent(@NonNull K k, @NonNull V v) {
        map.putIfAbsent(k, v);
    }

    @Override
    public void remove(@NonNull K k) {
        map.remove(k);
    }

    @Override
    public boolean containsKey(@NonNull K k) {
        return map.containsKey(k);
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
    public Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new MapStoreEntrySet(map.entrySet());
    }

    @Override
    public Map<K, V> asMap() {
        return map;
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> consumer) {
        map.forEach(consumer);
    }

    @Override
    public void traverse(BiFunction<? super K, ? super V, Boolean> traverser) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (!traverser.apply(entry.getKey(), entry.getValue()))
                break;
        }
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public Stream<Map.Entry<K, V>> stream() {
        return map.entrySet().stream();
    }

    @AllArgsConstructor
    private class MapStoreEntrySet implements Set<Map.Entry<K, V>> {
        private Set<Map.Entry<K, V>> delegate;

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return delegate.contains(o);
        }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return delegate.iterator();
        }

        @Override
        public Object[] toArray() {
            return delegate.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return delegate.toArray(a);
        }

        @Override
        public boolean add(Map.Entry<K, V> kvEntry) {
            if (kvEntry.getValue() == null || isTrap(kvEntry.getValue())) {
                return delegate.remove(kvEntry);
            }
            return delegate.add(kvEntry);
        }

        @Override
        public boolean remove(Object o) {
            return delegate.remove(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return delegate.containsAll(c);
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
            return delegate.retainAll(c);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return delegate.removeAll(c);
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        @Override
        public boolean equals(Object o) {
            return delegate.equals(o);
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        @Override
        public Spliterator<Map.Entry<K, V>> spliterator() {
            return delegate.spliterator();
        }
    }
}
