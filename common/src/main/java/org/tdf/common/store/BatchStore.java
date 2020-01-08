package org.tdf.common.store;

import java.util.Collection;
import java.util.Map;

/**
 * @param <K> key type
 * @param <V> value type
 * @author zhuyingjie
 */
public interface BatchStore<K, V> extends Store<K, V> {
    /**
     * Copies all of the mappings from rows to this store.
     * This operation should be atomic when writes to disk.
     * If null or trap value entry exists in rows, the entry will be removed.
     *
     * @param rows row to put into this store
     */
    void putAll(Collection<? extends Map.Entry<? extends K, ? extends V>> rows);
}
