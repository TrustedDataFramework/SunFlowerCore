package org.tdf.common.store;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 * abstract storage of key-value mappings
 *
 * @param <K> key
 * @param <V> value
 * @author zhuyingjie
 */
public interface Store<K, V> {
    /**
     * a trivial store implementation which is always empty
     */
    Store<?, ?> NONE = new Store() {
        @Override
        public Optional<?> get(Object o) {
            return Optional.empty();
        }

        @Override
        public void put(Object o, Object o2) {

        }

        @Override
        public void remove(Object o) {

        }

        @Override
        public void flush() {

        }


        @Override
        public void clear() {

        }

        @Override
        public Map<?, ?> asMap() {
            return Collections.emptyMap();
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public void traverse(BiFunction consumer) {
        }

        @Override
        public int size() {
            return 0;
        }
    };

    static <K, V> Store<K, V> getNop() {
        return (Store<K, V>) NONE;
    }

    /**
     * Gets a value by its key
     *
     * @param k key to query
     * @return value or empty if no such key in the source
     */
    Optional<V> get(K k);


    /**
     * put key with trap value will remove the key
     *
     * @return non null static trap value
     */
    default V getTrap() {
        throw new UnsupportedOperationException();
    }

    /**
     * Puts key-value pair into store
     *
     * @param k key of key-value mapping
     * @param v value of key-value mapping
     *          remove key in the store if {@code v == getTrap()}
     */
    void put(K k, V v);

    /**
     * Puts key-value pair into store when key not exists
     *
     * @param k key of key-value mapping
     * @param v value of key-value mapping
     */
    default void putIfAbsent(K k, V v) {
        if (containsKey(k)) return;
        put(k, v);
    }

    /**
     * Deletes the key-value mapping from the source
     *
     * @param k key of key-value mapping
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
     * Returns <tt>true</tt> if this store contains a mapping for the specified key.
     *
     * @param k key of key-value mapping
     * @return <tt>true</tt> if this store contains a mapping for the specified key.
     */
    default boolean containsKey(K k) {
        return get(k).isPresent();
    }

    /**
     * Returns <tt>true</tt> if this store contains no key-value mappings.
     *
     * @return <tt>true</tt> if this store contains no key-value mappings.
     */
    default boolean isEmpty() {
        boolean[] hasEntry = new boolean[1];
        traverse((k, v) -> {
            hasEntry[0] = true;
            return false;
        });
        return !hasEntry[0];
    }

    /**
     * Removes all of the mappings from this store (optional operation).
     * The store will be empty after this call returns.
     *
     * @throws UnsupportedOperationException if the <tt>clear</tt> operation
     *                                       is not supported by this store
     */
    void clear();

    /**
     * Performs the given action for each key value pair in this map store until all pairs
     * have been processed or the traverser return false
     *
     * @param traverser operation of key-value mapping, if traverser return false, the traverse will stop
     */
    void traverse(BiFunction<? super K, ? super V, Boolean> traverser);


    default void forEach(BiConsumer<? super K, ? super V> consumer) {
        traverse((k, v) -> {
            consumer.accept(k, v);
            return true;
        });
    }

    /**
     * Returns a {@link Set} view of the keys contained in this store.
     * The set is backed by the store, so changes to the store are
     * reflected in the set, and vice-versa.
     *
     * @return a set view of the keys contained in this store
     */
    default Set<K> keySet() {
        return new StoreKeySetView<>(this);
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice-versa.
     *
     * @return a collection view of the values contained in this store
     */
    default Collection<V> values() {
        return new StoreValuesView<>(this);
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this store.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.
     *
     * @return a set view of the mappings contained in this store
     */
    default Set<Map.Entry<K, V>> entrySet() {
        return new StoreEntrySetView<>(this);
    }

    /**
     * Returns the number of key-value mappings in this store.
     *
     * @return the number of key-value mappings in this store.
     */
    default int size() {
        int[] count = new int[1];
        traverse((k, v) -> {
            count[0]++;
            return true;
        });
        return count[0];
    }

    /**
     * Returns a sequential {@code Stream} with this mapping as its source.
     *
     * @return a sequential {@code Stream} over the elements in this collection
     */
    default Stream<Map.Entry<K, V>> stream() {
        Stream.Builder<Map.Entry<K, V>> builder = Stream.builder();
        traverse((k, v) -> {
            builder.accept(new AbstractMap.SimpleImmutableEntry<>(k, v));
            return true;
        });
        return builder.build();
    }

    /**
     * returns map view of store
     *
     * @return a map view of the mappings contained in this store
     */
    default Map<K, V> asMap() {
        return new StoreMapView<>(this);
    }
}
