package org.tdf.common.store;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RocksDBCachedStoreTests extends CachedStoreTests {
    private RocksDb db;

    @Override
    protected Store<byte[], byte[]> supplyDelegate() {
        db = new RocksDb("local/rocksdb", "tmp");
        db.init(DBSettings.DEFAULT);
        db.clear();
        return db;
    }

    @Override
    public void after() {
        db.close();
    }
}
