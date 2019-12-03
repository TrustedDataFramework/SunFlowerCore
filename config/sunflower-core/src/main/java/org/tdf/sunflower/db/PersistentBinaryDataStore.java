package org.tdf.sunflower.db;

import org.tdf.common.BatchAbleStore;
import org.tdf.common.DbSettings;

import java.util.Optional;

public interface PersistentBinaryDataStore extends BatchAbleStore<byte[], byte[]> {
    void init(DbSettings dbsettings);

    boolean isAlive();

    void close();

    void reset();

    Optional<byte[]> prefixLookup(byte[] key);
}
