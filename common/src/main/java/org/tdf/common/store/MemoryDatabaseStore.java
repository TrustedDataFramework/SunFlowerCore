package org.tdf.common.store;

import java.util.Iterator;
import java.util.Map;

public class MemoryDatabaseStore extends ByteArrayMapStore<byte[]> implements DatabaseStore {
    public MemoryDatabaseStore() {
    }

    public MemoryDatabaseStore(Map<byte[], byte[]> map) {
        super(map);
    }

    @Override
    public void init(DBSettings settings) {

    }

    @Override
    public boolean isAlive() {
        return true;
    }

    @Override
    public void close() {

    }
}
