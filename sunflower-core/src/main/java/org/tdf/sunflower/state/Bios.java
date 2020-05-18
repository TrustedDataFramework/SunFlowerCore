package org.tdf.sunflower.state;

import org.tdf.common.store.Store;
import org.tdf.sunflower.types.Header;

import java.util.Collections;
import java.util.Map;

public interface Bios {
    Account getGenesisAccount();

    default void update(
            Header header, Store<byte[], byte[]> contractStorage) {
        throw new IllegalArgumentException();
    }

    default Map<byte[], byte[]> getGenesisStorage() {
        return Collections.emptyMap();
    }
}
