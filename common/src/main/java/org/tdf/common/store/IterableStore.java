package org.tdf.common.store;

import java.util.Map;

public interface IterableStore<K, V> extends Store<K, V>, Iterable<Map.Entry<K, V>> {
}
