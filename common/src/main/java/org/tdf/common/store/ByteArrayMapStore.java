package org.tdf.common.store;

import org.tdf.common.util.ByteArrayMap;

import java.util.Map;

public class ByteArrayMapStore<V> extends MapStore<byte[], V> {
    public ByteArrayMapStore() {
        super(new ByteArrayMap<>());
    }


    public ByteArrayMapStore(Map<byte[], V> map) {
        super(map);
    }
}
