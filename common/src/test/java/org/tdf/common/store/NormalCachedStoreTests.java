package org.tdf.common.store;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.util.HexBytes;

@RunWith(JUnit4.class)
public class NormalCachedStoreTests extends CachedStoreTests {
    @Override
    protected Store<byte[], byte[]> supplyDelegate() {
        return new ByteArrayMapStoreWithTrap();
    }

    private static class ByteArrayMapStoreWithTrap extends ByteArrayMapStore<byte[]> {
        @Override
        public byte[] getTrap() {
            return HexBytes.EMPTY_BYTES;
        }

        @Override
        public boolean isTrap(byte[] bytes) {
            return bytes == getTrap() || bytes.length == 0;
        }
    }
}
