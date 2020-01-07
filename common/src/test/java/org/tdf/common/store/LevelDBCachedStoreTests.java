package org.tdf.common.store;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class LevelDBCachedStoreTests extends CachedStoreTests{
    private LevelDb db;

    @Override
    protected Store<byte[], byte[]> supplyDelegate() {
        db = new LevelDb("local/leveldb", "tmp");
        db.init(DBSettings.DEFAULT);
        db.clear();
        return db;
    }

    @Override
    public void after() {
        db.close();
    }
}