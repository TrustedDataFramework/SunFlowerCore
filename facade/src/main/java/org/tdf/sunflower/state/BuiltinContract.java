package org.tdf.sunflower.state;

import org.tdf.common.util.ByteUtil;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.facade.RepositoryReader;
import org.tdf.sunflower.vm.Backend;
import org.tdf.sunflower.vm.CallData;
import org.tdf.sunflower.vm.abi.Abi;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface BuiltinContract {
    HexBytes getAddress();

    default Map<HexBytes, HexBytes> getGenesisStorage() {
        return Collections.emptyMap();
    }

    default byte[] call(RepositoryReader rd, Backend backend, CallData callData) {
        return ByteUtil.EMPTY_BYTE_ARRAY;
    }

    default List<?> call(RepositoryReader rd, Backend backend, CallData callData, String method, Object... args) {
        return Collections.emptyList();
    }

    Abi getAbi();

    default Object view(RepositoryReader rd, HexBytes blockHash, String method, Object... args) {
        return null;
    }
}
