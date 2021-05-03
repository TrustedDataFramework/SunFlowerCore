package org.tdf.sunflower.state;

import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.vm.Backend;
import org.tdf.sunflower.vm.CallData;
import org.tdf.sunflower.vm.abi.Abi;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface CommonUpdater {
    Account getGenesisAccount();

    default Map<HexBytes, HexBytes> getGenesisStorage() {
        return Collections.emptyMap();
    }

    default byte[] call(Backend backend, CallData callData) {
        return HexBytes.EMPTY_BYTES;
    }

    Abi getAbi();
}
