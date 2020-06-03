package org.tdf.sunflower.facade;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.CryptoContext;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecretStoreImpl{
    private HexBytes publicKey;
    private HexBytes cipherText;

    public byte[] getPrivateKey(byte[] bobSk){
        byte[] key = CryptoContext.ecdh(false, bobSk, publicKey.getBytes());
        return CryptoContext.decrypt(key, cipherText.getBytes());
    }
}
