package org.tdf.common.store;

import lombok.NonNull;


/**
 * abstract storage of key-value mappings
 * the key and value are not allowed to be null
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
        public Object get(@NonNull Object o) {
            return null;
        }

        @Override
        public void put(@NonNull Object o, @NonNull Object o2) {

        }

        @Override
        public void remove(@NonNull Object o) {

        }

        @Override
        public void flush() {

        }
    };

    /**
     * return a trivial key-value store which is always empty
     *
     * @param <K> type of key
     * @param <V> type of value
     * @return trivial store
     */
    static <K, V> Store<K, V> getNop() {
        return (Store<K, V>) NONE;
    }

    /**
     * Gets a value by its key, return null or default value if key not found
     *
     * @param k key to query
     * @return value or
     */
    V get(K k);


    /**
     * puts key-value pair into store, remove key in the store if v is trap-like
     *
     * @param k key of key-value mapping
     * @param v value of key-value mapping
     */
    void put(K k, V v);


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

}
