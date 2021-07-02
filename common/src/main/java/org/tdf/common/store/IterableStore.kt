package org.tdf.common.store

interface IterableStore<K, V> : Store<K, V>, Iterable<Map.Entry<K, V>>