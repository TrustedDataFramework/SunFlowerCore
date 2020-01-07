package org.tdf.common.store;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.util.HexBytes;

import java.util.Map;
import java.util.Objects;

@RunWith(JUnit4.class)
public class NormalCachedStoreTests extends CachedStoreTests{
    @Override
    protected Store<byte[], byte[]> supplyDelegate() {
        return new ByteArrayMapStoreWithTrap();
    }

    private static class ByteArrayMapStoreWithTrap extends ByteArrayMapStore<byte[]>{
        @Override
        public byte[] getTrap() {
            return HexBytes.EMPTY_BYTES;
        }

        @Override
        public void put(byte[] bytes, byte[] bytes2) {
            Objects.requireNonNull(bytes);
            Objects.requireNonNull(bytes2);
            if(bytes2 == getTrap()) {
                getMap().remove(bytes);
                return;
            }
            getMap().put(bytes, bytes2);
        }

        @Override
        public void putAll(Map<byte[], byte[]> rows) {
            rows.forEach((k, v) -> {
                if(v == null) {
                    remove(k);
                    return;
                }
                put(k, v);
            });
        }
    }
}
