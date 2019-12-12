package org.tdf.sunflower.db;

import org.tdf.store.ByteArrayMapStore;
import org.tdf.store.DatabaseStore;
import org.tdf.store.DbSettings;

import java.util.Map;
import java.util.Optional;

public class MemoryDatabaseStore extends ByteArrayMapStore<byte[]> implements DatabaseStore {
    @Override
    public void init(DbSettings settings) {

    }

    @Override
    public boolean isAlive() {
        return true;
    }

    @Override
    public void close() {

    }

    @Override
    public Optional<byte[]> prefixLookup(byte[] key, int prefixBytes) {
        throw new RuntimeException("not supported");
    }

    @Override
    public void putAll(Map<byte[], byte[]> rows) {
        getMap().putAll(rows);
    }
}
