/*
 * Copyright (c) [2019] [ <silk chain> ]
 * This file is part of the silk chain library.
 *
 * The silk chain library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The silk chain library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the silk chain library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tdf.sunflower.consensus.vrf.keystore;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.spongycastle.crypto.generators.SCrypt;
import org.spongycastle.jcajce.provider.digest.Keccak;
import org.spongycastle.util.encoders.Hex;
import org.tdf.crypto.PrivateKey;
import org.tdf.crypto.ed25519.Ed25519PrivateKey;
import org.tdf.sunflower.consensus.vrf.HashUtil;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.util.Arrays;
import java.util.UUID;

/**
 * Converts private key and password to json content and vise versa.
 *
 * @author James Hu
 * @since 2019/6/15
 */
public class KeystoreFormat {

    public String toKeystore(final PrivateKey key, String password) {
        try {
            // n,r,p = 2^18, 8, 1 uses 256MB memory and approx 1s CPU time on a modern CPU.
//            final int ScryptN = ((Double) Math.pow(10.0, 18.0)).intValue();
            final int ScryptN = 262144;
            final int ScryptR = 8;
            final int ScryptP = 1;
            final int ScryptDklen = 32;
            // salt
            final byte[] salt = generateRandomBytes(32);


            final byte[] derivedKey = scrypt(password.getBytes(), salt, ScryptN, ScryptR, ScryptP, ScryptDklen);

            // 128-bit initialisation vector for the cipher (16 bytes)
            final byte[] iv = generateRandomBytes(16);
            final byte[] privateKey = key.getEncoded();
            final byte[] encryptKey = Arrays.copyOfRange(derivedKey, 0, 16);
            final byte[] cipherText = encryptAes(iv, encryptKey, privateKey);
            final byte[] mac = HashUtil.sha3(concat(Arrays.copyOfRange(derivedKey, 16, 32), cipherText));


            final KeystoreItem keystore = new KeystoreItem();
            keystore.pubkey = Hex.toHexString(key.generatePublicKey().getEncoded());
            keystore.id = UUID.randomUUID().toString();
            keystore.version = 3;
            keystore.crypto = new KeystoreCrypto();
            keystore.crypto.setKdf("scrypt");
            keystore.crypto.setMac(Hex.toHexString(mac));
            keystore.crypto.setCipher("aes-128-ctr");
            keystore.crypto.setCiphertext(Hex.toHexString(cipherText));
            keystore.crypto.setCipherparams(new CipherParams());
            keystore.crypto.getCipherparams().setIv(Hex.toHexString(iv));
            keystore.crypto.setKdfparams(new KdfParams());
            keystore.crypto.getKdfparams().setN(ScryptN);
            keystore.crypto.getKdfparams().setR(ScryptR);
            keystore.crypto.getKdfparams().setP(ScryptP);
            keystore.crypto.getKdfparams().setDklen(ScryptDklen);
            keystore.crypto.getKdfparams().setSalt(Hex.toHexString(salt));

            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(keystore);
        } catch (Exception e) {
            throw new RuntimeException("Problem storing key. Message: " + e.getMessage(), e);
        }
    }

    private byte[] generateRandomBytes(int size) {
        final byte[] bytes = new byte[size];
        SecureRandom random = new SecureRandom();
        random.nextBytes(bytes);
        return bytes;
    }


    public PrivateKey fromKeystore(final String content, final String password) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            final KeystoreItem keystore = mapper.readValue(content, KeystoreItem.class);
            final byte[] cipherKey;

            if (keystore.version != 3) {
                throw new RuntimeException("Keystore version 3 only supported.");
            }

            switch (keystore.getCrypto().getKdf()) {
                case "pbkdf2":
                    cipherKey = checkMacSha3(keystore, password);
                    break;
                case "scrypt":
                    cipherKey = checkMacScrypt(keystore, password);
                    break;
                default:
                    throw new RuntimeException("non valid algorithm " + keystore.getCrypto().getCipher());
            }

            byte[] privateKey = decryptAes(
                Hex.decode(keystore.getCrypto().getCipherparams().getIv()),
                cipherKey,
                Hex.decode(keystore.getCrypto().getCiphertext())
            );

            return new Ed25519PrivateKey(privateKey);
        } catch (Exception e) {
            throw new RuntimeException("Cannot unlock account. Message: " + e.getMessage(), e);
        }
    }

    private byte[] decryptAes(byte[] iv, byte[] keyBytes, byte[] cipherText) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        return processAes(iv, keyBytes, cipherText, Cipher.DECRYPT_MODE);
    }

    private byte[] encryptAes(byte[] iv, byte[] keyBytes, byte[] cipherText) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        return processAes(iv, keyBytes, cipherText, Cipher.ENCRYPT_MODE);
    }

    private byte[] processAes(byte[] iv, byte[] keyBytes, byte[] cipherText, int encryptMode) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        // Mode
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");

        cipher.init(encryptMode, key, ivSpec);
        return cipher.doFinal(cipherText);
    }

    private byte[] checkMacSha3(KeystoreItem keystore, String password) throws Exception {
        byte[] salt = Hex.decode(keystore.getCrypto().getKdfparams().getSalt());
        int iterations = keystore.getCrypto().getKdfparams().getC();
        byte[] part = new byte[16];
        byte[] h = hash(password, salt, iterations);
        byte[] cipherText = Hex.decode(keystore.getCrypto().getCiphertext());
        System.arraycopy(h, 16, part, 0, 16);

        byte[] actual = sha3(concat(part, cipherText));

        if (Arrays.equals(actual, Hex.decode(keystore.getCrypto().getMac()))) {
            System.arraycopy(h, 0, part, 0, 16);
            return part;
        }

        throw new RuntimeException("Most probably a wrong passphrase");
    }

    private byte[] checkMacScrypt(KeystoreItem keystore, String password) throws Exception {
        byte[] part = new byte[16];
        KdfParams params = keystore.getCrypto().getKdfparams();
        byte[] h = scrypt(password.getBytes(), Hex.decode(params.getSalt()), params.getN(), params.getR(), params.getP(), params.getDklen());
        byte[] cipherText = Hex.decode(keystore.getCrypto().getCiphertext());
        System.arraycopy(h, 16, part, 0, 16);

        byte[] actual = sha3(concat(part, cipherText));

        if (Arrays.equals(actual, Hex.decode(keystore.getCrypto().getMac()))) {
            System.arraycopy(h, 0, part, 0, 16);
            return part;
        }

        throw new RuntimeException("Most probably a wrong passphrase");
    }

    private byte[] concat(byte[] a, byte[] b) {
        int aLen = a.length;
        int bLen = b.length;
        byte[] c = new byte[aLen + bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }

    private byte[] scrypt(byte[] pass, byte[] salt, int n, int r, int p, int dkLen) throws GeneralSecurityException {
        return SCrypt.generate(pass, salt, n, r, p, dkLen);
    }

    private byte[] hash(String encryptedData, byte[] salt, int iterations) throws Exception {
        char[] chars = encryptedData.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, 256);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return skf.generateSecret(spec).getEncoded();
    }

    private byte[] sha3(byte[] h) throws NoSuchAlgorithmException {
        MessageDigest KECCAK = new Keccak.Digest256();
        KECCAK.reset();
        KECCAK.update(h);
        return KECCAK.digest();
    }
}
