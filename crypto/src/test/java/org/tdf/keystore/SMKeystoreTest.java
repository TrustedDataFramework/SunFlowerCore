package org.tdf.keystore;

import org.junit.Test;
import org.tdf.crypto.keystore.SMKeystore;
public class SMKeystoreTest {

    @Test
    public void testKeyStore() {
        String keyJson = SMKeystore.generateKeystore("123456", "626cb1df7c489a09243ecb4d7f12ffad61cd1b7863331ac7de7364810616e14a");
        System.out.println(keyJson);
        String key = SMKeystore.decryptKeyStore(keyJson, "123456");
        System.out.println(key);

    }

}
