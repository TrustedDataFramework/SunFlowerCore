package org.tdf.common.store;

import org.tdf.common.util.ByteArrayMap;

import java.util.Map;

public class ByteArrayMapStore<V> extends MapStore<byte[], V> {
    public ByteArrayMapStore() {
        super(new ByteArrayMap<>());
    }

    public ByteArrayMapStore(Store<byte[], V> store) {
        this();
        store.forEach(super::put);
    }

    public ByteArrayMapStore(Map<byte[], V> map) {
        super(map);
    }
}
