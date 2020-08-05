package org.tdf.sunflower.facade;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tdf.common.util.HexBytes;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecretStoreImpl{
    private HexBytes publicKey;
    private HexBytes cipherText;
}
