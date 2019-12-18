package org.tdf.common.store;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public interface Store<K, V> {

    /**
     * Gets a value by its key
     *
     * @return value or empty if no such key in the source
     */
    Optional<V> get(K k);

    /**
     * Puts key-value pair into store
     */
    void put(K k, V v);

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

    Set<K> keySet();

    Collection<V> values();

    boolean containsKey(K k);

    int size();

    boolean isEmpty();

    void clear();

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
    };

    static <K, V> Store<K, V> getNop() {
        return (Store<K, V>) NONE;
    }
}
