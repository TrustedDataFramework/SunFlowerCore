package org.tdf.common.store;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RocksDBTests extends DBTests {
    @Override
    DatabaseStore getDB() {
        return new RocksDb("local/rocksdb", "tmp");
    }
}
