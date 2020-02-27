package org.tdf.sunflower;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.crypto.KeyPair;
import org.tdf.crypto.sm2.SM2;

@RunWith(JUnit4.class)
public class GMTests {

    @Test
    public void test(){
        KeyPair keyPair = SM2.generateKeyPair();
        System.out.println(keyPair.getPublicKey().getEncoded().length);
    }
}
