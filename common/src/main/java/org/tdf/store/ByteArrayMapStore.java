package org.tdf.store;

import org.tdf.common.HexBytes;
import org.tdf.util.ByteArrayMap;
import org.tdf.util.ExceptionUtil;

import java.util.Map;

public class ByteArrayMapStore<V> extends MapStore<byte[], V> implements Store<byte[], V> {
    public ByteArrayMapStore() {
        super(new ByteArrayMap<>());
    }

    public ByteArrayMapStore(Store<byte[], V> store){
        this();
        for(byte[] k: store.keySet()){
            put(k, store.get(k).orElseThrow(() -> ExceptionUtil.keyNotFound(HexBytes.encode(k))));
        }
    }

    public ByteArrayMapStore(Map<byte[], V> map){
        this();
        for(byte[] k: map.keySet()){
            put(k, map.get(k));
        }
    }
}
