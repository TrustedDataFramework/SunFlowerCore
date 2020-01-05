package org.tdf.common.store;

import java.util.Map;

public interface BatchStore<K, V> extends Store<K, V> {
    /**
     * Copies all of the mappings from rows to this store.
     * This operation should be atomic when writes to disk.
     * If null value entry exists in rows, the entry will be removed.
     */
    void putAll(Map<K, V> rows);
}
