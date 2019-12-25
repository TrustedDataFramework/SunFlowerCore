package org.tdf.common.store;

import java.util.Map;

public interface BatchStore<K, V> extends Store<K, V> {
    // put key-value pair to the store
    // if the reference of value equals to getNull()
    // the key will be removed
    void putAll(Map<K, V> rows);
}
