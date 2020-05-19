package org.tdf.sunflower.state;

import org.tdf.common.store.Store;
import org.tdf.sunflower.types.Header;

public interface Bios extends CommonUpdater{
    default void update(
            Header header, Store<byte[], byte[]> contractStorage) {
        throw new IllegalArgumentException();
    }
}
