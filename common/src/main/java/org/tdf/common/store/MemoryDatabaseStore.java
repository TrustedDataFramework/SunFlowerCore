package org.tdf.common.store;

import lombok.NonNull;

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
        rows.forEach((k, v) -> {
            if (v == null || v == EMPTY || v.length == 0) {
                getMap().remove(k);
                return;
            }
            getMap().put(k, v);
        });
    }

    @Override
    public void put(byte @NonNull [] k, byte @NonNull [] v) {
        if (v == EMPTY || v.length == 0) {
            remove(k);
            return;
        }
        super.put(k, v);
    }
}
