package org.tdf.crypto.keystore;

import lombok.NonNull;
import lombok.SneakyThrows;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import org.tdf.common.util.HexBytes;
import org.tdf.crypto.PrivateKey;
import org.tdf.crypto.PublicKey;
import org.tdf.crypto.sm2.SM2;
import org.tdf.crypto.sm2.SM2PrivateKey;
import org.tdf.gmhelper.SM3Util;
import org.tdf.gmhelper.SM4Util;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.UUID;

import static org.tdf.crypto.keystore.KeyStoreImpl.*;

public class SMKeystore {

    public String address;
    public Crypto crypto;

    @SneakyThrows
    public static byte[] decryptKeyStore(KeyStoreImpl ks, String password) {
        if (!"sm4-128-ecb".equals(ks.getCrypto().getCipher())) {
            throw new RuntimeException("unsupported crypto cipher " + ks.getCrypto().getCipher());
        }
        HexBytes passwordBytes = HexBytes.fromBytes(password.getBytes(StandardCharsets.US_ASCII));

        byte[] deriveKey = ByteUtils.subArray(
            SM3Util.hash(
                ByteUtils.concatenate(ks.getCrypto().getSalt().getBytes(), passwordBytes.getBytes())
            )
            , 0, 16
        );
        byte[] cipherPrivKey = ks.getCrypto().getCipherText().getBytes();
        byte[] iv = ks.getCrypto().getIv().getBytes();
        return SM4Util.decrypt_Ecb_NoPadding(deriveKey, cipherPrivKey);
    }

    public static KeyStoreImpl generateKeyStore(@NonNull String password) {
        return generateKeyStore(password, SM2.generateKeyPair().getPrivateKey().getEncoded());
    }

    @SneakyThrows
    public static KeyStoreImpl generateKeyStore(String password, byte[] privateKey) {
        PrivateKey sm2PrivateKey = new SM2PrivateKey(privateKey);
        PublicKey publicKey = sm2PrivateKey.generatePublicKey();
        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[IV_LENGTH];

        SecureRandom sr = new SecureRandom();
        sr.nextBytes(salt);
        sr.nextBytes(iv);

        byte[] deriveKey = ByteUtils.subArray(
            SM3Util.hash(
                ByteUtils.concatenate(salt, password.getBytes(StandardCharsets.US_ASCII))), 0, 16);

        // sm2 的私钥是 32 个字节，正好是 16的倍数，所以不需要填充
        byte[] cipherPrivKey = SM4Util.encrypt_Ecb_NoPadding(deriveKey, sm2PrivateKey.getEncoded());

        byte[] mac = SM3Util.hash(ByteUtils.concatenate(deriveKey, cipherPrivKey));
        Crypto crypto = new Crypto("sm4-128-ecb", HexBytes.fromBytes(cipherPrivKey), HexBytes.fromBytes(iv), HexBytes.fromBytes(salt));

        return new KeyStoreImpl(
            HexBytes.fromBytes(publicKey.getEncoded()),
            crypto,
            UUID.randomUUID().toString(),
            DEFAULT_VERSION,
            HexBytes.fromBytes(mac),
            "sm2-kdf",
            HexBytes.fromBytes(privateKey)
        );
    }
}
