package org.tdf.sunflower.state;

import java.util.Collections;
import java.util.Map;

public interface CommonUpdater {
    Account getGenesisAccount();

    default Map<byte[], byte[]> getGenesisStorage() {
        return Collections.emptyMap();
    }
}
