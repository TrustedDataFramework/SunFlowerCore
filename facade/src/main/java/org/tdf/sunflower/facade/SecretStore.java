package org.tdf.sunflower.facade;

import org.tdf.common.util.HexBytes;

public interface SecretStore {
    SecretStore NONE = () -> HexBytes.EMPTY;

    HexBytes getPrivateKey();
}
