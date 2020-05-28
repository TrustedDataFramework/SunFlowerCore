package org.tdf.keystore;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.tdf.common.util.HexBytes;
import org.tdf.crypto.keystore.Keystore;
import org.tdf.crypto.keystore.SMKeystore;
public class SMKeystoreTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void testKeyStore() throws Exception{
        Keystore keyJson = SMKeystore.generateKeyStore("123456", HexBytes.decode("626cb1df7c489a09243ecb4d7f12ffad61cd1b7863331ac7de7364810616e14a"));
        System.out.println(MAPPER.writeValueAsString(keyJson));
        byte[] key = SMKeystore.decryptKeyStore(keyJson, "123456");
        System.out.println(HexBytes.fromBytes(key));

    }

}
