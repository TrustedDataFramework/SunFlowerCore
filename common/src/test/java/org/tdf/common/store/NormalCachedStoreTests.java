package org.tdf.common.store;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NormalCachedStoreTests extends CachedStoreTests{
    @Override
    protected Store<byte[], byte[]> supplyDelegate() {
        return new ByteArrayMapStore<>();
    }
}
