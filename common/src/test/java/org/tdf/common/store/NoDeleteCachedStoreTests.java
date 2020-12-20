package org.tdf.common.store;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.util.ByteArrayMap;

@RunWith(JUnit4.class)
public class NoDeleteCachedStoreTests extends NoDeleteStoreTest {
    @Override
    protected Store<byte[], byte[]> supplyNoDelete() {
        return new CachedStore<>(new NoDeleteBatchStore<>(new MemoryDatabaseStore()), ByteArrayMap::new);
    }
}
