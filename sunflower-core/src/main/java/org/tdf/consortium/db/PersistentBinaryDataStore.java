package org.wisdom.consortium.db;

import org.wisdom.common.BatchAbleStore;
import org.wisdom.common.DbSettings;

import java.util.Optional;

public interface PersistentBinaryDataStore extends BatchAbleStore<byte[], byte[]> {
    void init(DbSettings dbsettings);

    boolean isAlive();

    void close();

    void reset();

    Optional<byte[]> prefixLookup(byte[] key);
}
