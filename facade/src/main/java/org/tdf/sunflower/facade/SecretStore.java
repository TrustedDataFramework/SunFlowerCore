package org.tdf.sunflower.facade;

import org.tdf.common.util.HexBytes;

public interface SecretStore {
    HexBytes getPrivateKey();

    SecretStore NONE = () -> HexBytes.EMPTY;
}
