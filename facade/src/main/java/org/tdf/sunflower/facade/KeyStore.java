package org.tdf.sunflower.facade;

import org.tdf.common.util.HexBytes;

public interface KeyStore {
    HexBytes getPrivateKey();

    KeyStore NONE = () -> HexBytes.EMPTY;
}
