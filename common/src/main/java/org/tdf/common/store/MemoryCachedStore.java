package org.tdf.common.store;

import org.tdf.common.util.ByteArrayMap;

import java.util.Map;

public class MemoryCachedStore extends CachedStore<byte[], byte[]> {
    static byte[] trap = new byte[0];
    public MemoryCachedStore(Store<byte[], byte[]> delegated) {
        super(delegated);
    }

    @Override
    Map<byte[], byte[]> newCache() {
        return new ByteArrayMap<>();
    }

    @Override
    byte[] getTrap(){
      return trap;
    }
}
