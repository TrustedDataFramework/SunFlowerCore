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
package org.tdf.sunflower.vrf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.tdf.crypto.PrivateKey;
import org.tdf.crypto.ed25519.Ed25519;
import org.tdf.sunflower.consensus.vrf.keystore.FileSystemKeystore;
import org.tdf.sunflower.consensus.vrf.keystore.Keystore;
import org.tdf.sunflower.consensus.vrf.struct.VrfPrivateKey;

/**
 * @author James Hu
 * @since 2019/6/15
 */
@Ignore("not pass")
public class KeyStoreImplTest {

    /**
     * Keystore which uses temp dir instead of real user keystore dir
     */
    private static Keystore fileSystemKeystore;

    @BeforeClass
    public static void setup() {
        Path keystorePath = null;
        try {
            keystorePath = Files.createTempDirectory("keystore");
        } catch (IOException e) {
            e.printStackTrace();
        }

        fileSystemKeystore = new FileSystemKeystore(keystorePath.toString());

        System.out.println("Keystore Path: " + keystorePath.toString());
    }

    @AfterClass
    public static void cleanup() {
        Path keystorePath = fileSystemKeystore.getKeyStoreLocation();
        try {
            Files.delete(keystorePath);
        } catch (java.io.IOException ex) {
            System.out.println("Fail to delete Path");
        }
    }

    @Ignore // not passed
    @Test
    public void encodeDecode() throws Exception {
        final String password = "123";

        // generate new random private key
        final PrivateKey key = new VrfPrivateKey(Ed25519.getAlgorithm()).getSigner();
        ;
        final String pubkey = Hex.toHexString(key.generatePublicKey().getEncoded());

        fileSystemKeystore.removeKey(pubkey);
        fileSystemKeystore.storeKey(key, password);

        final PrivateKey keyDecode = fileSystemKeystore.loadStoredKey(pubkey, password);

        fileSystemKeystore.removeKey(pubkey);

        assertEquals(Hex.toHexString(key.getEncoded()), Hex.toHexString(keyDecode.getEncoded()));
    }

    @Ignore("not pass")
    @Test
    public void readCorrectKey() throws Exception {
        final String password = "123";
        final String pubkey = "aa61e3f3239bc8e647ec9593167e729857a6a05ab9f59b8d1a906f07d8f576e2";

        fileSystemKeystore.removeKey(pubkey);
        fileSystemKeystore.storeRawKeystore(CORRECT_KEY, pubkey);

        final PrivateKey key = fileSystemKeystore.loadStoredKey(pubkey, password);

        fileSystemKeystore.removeKey(pubkey);

        assertNotNull(key);
    }

    @Test(expected = RuntimeException.class)
    public void readCorrectKeyWrongPassword() throws Exception {
        final String password = "123456";
        final String pubkey = "aa61e3f3239bc8e647ec9593167e729857a6a05ab9f59b8d1a906f07d8f576e2";

        fileSystemKeystore.removeKey(pubkey);
        fileSystemKeystore.storeRawKeystore(CORRECT_KEY, pubkey);

        try {
            fileSystemKeystore.loadStoredKey(pubkey, password);
        } finally {
            fileSystemKeystore.removeKey(pubkey);
        }
    }

    @Test(expected = RuntimeException.class)
    public void importDuplicateKey() throws Exception {
        // generate new random private key
        final PrivateKey key = new VrfPrivateKey(Ed25519.getAlgorithm()).getSigner();
        ;
        final String pubkey = Hex.toHexString(key.generatePublicKey().getEncoded());

        try {
            fileSystemKeystore.storeKey(key, pubkey);
            fileSystemKeystore.storeKey(key, pubkey);
        } finally {
            fileSystemKeystore.removeKey(pubkey);
        }
    }

    private static String CORRECT_KEY = "{\"pubkey\":\"aa61e3f3239bc8e647ec9593167e729857a6a05ab9f59b8d1a906f07d8f576e2\",\"crypto\":{\"cipher\":\"aes-128-ctr\",\"ciphertext\":\"2aae2acddad8849593dde27132692f8e11a2c9de0824ad42f41dae29c421b32d\",\"cipherparams\":{\"iv\":\"d73651672e9a0e71482876a4003ef68f\"},\"kdf\":\"scrypt\",\"kdfparams\":{\"dklen\":32,\"n\":262144,\"p\":1,\"r\":8,\"salt\":\"b7c23f0d5e306ab8c6410b7158603b22ab41a67b65ef946cba07bfd8845eb8f3\"},\"mac\":\"5b7eaeecc0b420a46c419a3d7a83799c7eaf33f50c4a76bc54dcf1258da706c3\"},\"id\":\"f1a6fb4f-11d9-435b-a2f6-8956424c9b47\",\"version\":3}";
}