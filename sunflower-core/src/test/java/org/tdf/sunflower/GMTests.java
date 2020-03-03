package org.tdf.sunflower;


import org.apache.commons.codec.binary.Hex;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.crypto.KeyPair;
import org.tdf.crypto.sm2.SM2;
import org.tdf.gmhelper.SM3Util;
import org.tdf.sunflower.account.Address;

import java.util.Arrays;
import java.util.PriorityQueue;

@RunWith(JUnit4.class)
public class GMTests {

    @Test
    public void test(){
        KeyPair keyPair = SM2.generateKeyPair();
        System.out.println(keyPair.getPublicKey().getEncoded().length);
    }

    @Test
    public void test1(){
        PriorityQueue<Long> q = new PriorityQueue<>(Long::compare);
        q.add(1L);
        q.add(1L);
        assert q.size() == 2;
    }

    @Test
    public void test3(){
        for(int i = 0; i < 7; i++){
            KeyPair kp = SM2.generateKeyPair();
            String sk = Hex.encodeHexString(kp.getPrivateKey().getEncoded());
            String pk = Hex.encodeHexString(kp.getPublicKey().getEncoded());
            byte[] pkhash = SM3Util.hash(kp.getPublicKey().getEncoded());
            String address = Hex.encodeHexString(Arrays.copyOfRange(pkhash, pkhash.length - 20, pkhash.length));
            System.out.println(
                    String.format("accounts sk: %s pk: %s address %s", sk , pk ,address)
            );
        }
    }
}
