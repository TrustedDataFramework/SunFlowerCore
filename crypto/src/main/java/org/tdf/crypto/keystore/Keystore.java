package org.tdf.crypto.keystore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tdf.common.util.HexBytes;

@NoArgsConstructor
@Data
@AllArgsConstructor
public class Keystore {
    static final int SALT_LENGTH = 32;
    static final int IV_LENGTH = 16;
    static final String DEFAULT_VERSION = "1";

    private HexBytes publicKey;
    private Crypto crypto;
    private String id;
    private String version;
    private HexBytes mac;
    private String kdf;
}
