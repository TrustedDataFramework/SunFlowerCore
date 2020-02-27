package org.tdf.sunflower;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.crypto.KeyPair;
import org.tdf.crypto.sm2.SM2;

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
}
