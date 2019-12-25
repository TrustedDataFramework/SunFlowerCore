package org.tdf.common.store;

import java.util.*;

/**
 * abstract storage of key-value pairs
 * @param <K> key
 * @param <V> value
 */
public interface Store<K, V> {
    /**
     * Gets a value by its key
     * @return value or empty if no such key in the source
     */
    Optional<V> get(K k);

    /**
     * Puts key-value pair into store
     * remove key in the store if puts empty value
     */
    void put(K k, V v);

    /**
     * Puts key-value pair into store when key not exists
     */
    void putIfAbsent(K k, V v);

    /**
     * Deletes the key-value pair from the source
     */
    void remove(K k);

    /**
     * If this source has underlying level source then all
     * changes collected in this source are flushed into the
     * underlying source.
     * The implementation may do 'cascading' flush, i.e. call
     * Source didn't change
     */
    void flush();

    /**
     * traverse the store and get all keys in the store
     * @return keys in the store
     */
    Set<K> keySet();

    /**
     * traverse the store and get all values in the store
     * @return values in the store
     */
    Collection<V> values();

    boolean containsKey(K k);

    int size();

    boolean isEmpty();

    void clear();

    /**
     * copy all key-value pairs into a map
     * the map behaves identically to the store
     * @return map copied
     */
    Map<K, V> asMap();

    Store<?, ?> NONE = new Store() {
        @Override
        public Optional<?> get(Object o) {
            return Optional.empty();
        }

        @Override
        public void put(Object o, Object o2) {

        }

        @Override
        public void putIfAbsent(Object o, Object o2) {

        }

        @Override
        public void remove(Object o) {

        }

        @Override
        public void flush() {

        }

        @Override
        public Set<?> keySet() {
            return Collections.emptySet();
        }

        @Override
        public Collection values() {
            return Collections.emptyList();
        }

        @Override
        public boolean containsKey(Object o) {
            return false;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public void clear() {

        }

        @Override
        public Map asMap() {
            return new HashMap();
        }
    };

    static <K, V> Store<K, V> getNop() {
        return (Store<K, V>) NONE;
    }
}
