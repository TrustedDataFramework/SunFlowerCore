package org.tdf.common.store;

import java.util.Map;

/**
 * @author zhuyingjie
 * @param <K> key type
 * @param <V> value type
 */
public interface BatchStore<K, V> extends Store<K, V> {
    /**
     * Copies all of the mappings from rows to this store.
     * This operation should be atomic when writes to disk.
     * If null value entry exists in rows, the entry will be removed.
     * @param rows row to put into this store
     */
    void putAll(Map<K, V> rows);
}
