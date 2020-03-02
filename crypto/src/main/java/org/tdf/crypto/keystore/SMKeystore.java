package org.tdf.crypto.keystore;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import org.tdf.crypto.KeyPair;
import org.tdf.crypto.PrivateKey;
import org.tdf.crypto.PublicKey;
import org.tdf.crypto.sm2.SM2;
import org.tdf.crypto.sm2.SM2PrivateKey;
import org.tdf.gmhelper.SM3Util;
import org.tdf.gmhelper.SM4Util;

import java.security.*;
import java.util.UUID;

public class SMKeystore {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public String address;
    public Crypto crypto;
    private static final int saltLength = 32;
    private static final int ivLength = 16;
    private static final String defaultVersion = "1";

    public static String generateKeystore(String passwd, String privKey) {
        KeyPair keyPair = null;
        if (privKey == null) {
            keyPair = SM2.generateKeyPair();
            privKey = ByteUtils.toHexString(keyPair.getPrivateKey().getEncoded());
        }
        return generateKeyStore(passwd, privKey);
    }


    @SneakyThrows
    public static String decryptKeyStore(String jsonString, String passwd) {
        Keystore ks = OBJECT_MAPPER.readValue(jsonString, Keystore.class);
        if (!"sm4-128-ctr".equals(ks.crypto.getCipher()) ||
            !"sm2-kdf".equals(ks.getKdf())) {
            return null;
        }
        byte[] deriveKey = ByteUtils.subArray(SM3Util.hash(ByteUtils.concatenate(ByteUtils.fromHexString(ks.kdfparams.salt), passwd.getBytes())), 0, 16);
        byte[] cipherPrivKey = ByteUtils.fromHexString(ks.crypto.ciphertext);
        byte[] iv = ByteUtils.fromHexString(ks.crypto.cipherparams.iv);
        byte[] privKey = SM4Util.decrypt_Ctr_NoPadding(deriveKey, iv, cipherPrivKey);
        return ByteUtils.toHexString(privKey);
    }

    @SneakyThrows
    private static String generateKeyStore(String passwd, String privKey) {
        PrivateKey sm2PrivateKey = new SM2PrivateKey(ByteUtils.fromHexString(privKey));
        PublicKey publicKey = sm2PrivateKey.generatePublicKey();
        byte[] salt = new byte[saltLength];
        byte[] iv = new byte[ivLength];

        SecureRandom sr = new SecureRandom();
        sr.nextBytes(salt);
        sr.nextBytes(iv);

        byte[] deriveKey = ByteUtils.subArray(SM3Util.hash(ByteUtils.concatenate(salt, passwd.getBytes())), 0, 16);
        byte[] cipherPrivKey = SM4Util.encrypt_Ctr_NoPadding(deriveKey, iv, sm2PrivateKey.getEncoded());

        byte[] mac = SM3Util.hash(ByteUtils.concatenate(deriveKey, cipherPrivKey));
        Crypto crypto = new Crypto("sm4-128-ctr", ByteUtils.toHexString(cipherPrivKey), new Cipherparams(ByteUtils.toHexString(iv)));
        Kdfparams kdfparams = new Kdfparams(ByteUtils.toHexString(salt));

        Keystore ks = new Keystore(ByteUtils.toHexString(publicKey.getEncoded()), crypto, UUID.randomUUID().toString(),
                defaultVersion, ByteUtils.toHexString(mac), "sm2-kdf", kdfparams);
        return OBJECT_MAPPER.writeValueAsString(ks);
    }
}
