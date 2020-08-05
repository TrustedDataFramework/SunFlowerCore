package org.tdf.sunflower.state;

import org.tdf.sunflower.types.Block;

import java.util.Collections;
import java.util.Map;

public interface CommonUpdater {
    Account getGenesisAccount();
    default Map<byte[], byte[]> getGenesisStorage() {
        return Collections.emptyMap();
    }
}
