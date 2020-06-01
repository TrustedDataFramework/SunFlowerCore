package org.tdf.sunflower.facade;

import org.tdf.common.store.DatabaseStore;

public interface DatabaseStoreFactory {
    String getDirectory();

    DatabaseStore create(String name);

    void cleanup();
}
