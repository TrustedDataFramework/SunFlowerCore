package org.tdf.crypto.keystore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tdf.common.util.HexBytes;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Crypto {
    private String cipher;
    private HexBytes cipherText;
    private HexBytes iv;
    private HexBytes salt;
}
