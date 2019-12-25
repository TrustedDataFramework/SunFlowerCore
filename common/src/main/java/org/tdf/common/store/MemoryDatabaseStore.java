package org.tdf.common.store;

import org.tdf.common.util.ByteArrayMap;

import java.util.Map;
import java.util.Optional;

public class MemoryDatabaseStore extends ByteArrayMapStore<byte[]> implements DatabaseStore {
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

    @Override
    public Optional<byte[]> prefixLookup(byte[] key, int prefixBytes) {
        throw new RuntimeException("not supported");
    }

    @Override
    public void putAll(Map<byte[], byte[]> rows) {
        getMap().putAll(rows);
    }
}
