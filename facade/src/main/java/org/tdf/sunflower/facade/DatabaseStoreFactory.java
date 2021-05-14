package org.tdf.sunflower.facade;

import org.tdf.common.store.Store;

public interface DatabaseStoreFactory {
    String getDirectory();

    Store<byte[], byte[]> create(char prefix);

    default void cleanup() {

    }

    String getName();
}
