package org.tdf.sunflower;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Hex;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.util.HexBytes;
import org.tdf.crypto.KeyPair;
import org.tdf.crypto.sm2.SM2;
import org.tdf.crypto.sm2.SM2PrivateKey;
import org.tdf.gmhelper.SM2Util;
import org.tdf.gmhelper.SM3Util;
import org.tdf.sunflower.crypto.CryptoContext;

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
    @Ignore
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

    @Test
    @Ignore
    public void test4(){
        KeyPair kp1 = SM2.generateKeyPair();
        KeyPair kp2 = SM2.generateKeyPair();
        int count = 100000;
        long start = System.currentTimeMillis();
        for(int i = 0; i < count; i++){
            byte[] s1 = CryptoContext.ecdh(true, kp1.getPrivateKey().getEncoded(), kp2.getPublicKey().getEncoded());
        }
        long end = System.currentTimeMillis();
        System.out.println( (end - start) * 1.0 /(count));
    }

    @Test
    @SneakyThrows
//    @Ignore
    public void test5(){
        Cache<HexBytes, byte[]> cache = CacheBuilder.newBuilder()
                .maximumSize(1024)
                .build();

        KeyPair kp1 = SM2.generateKeyPair();
        KeyPair kp2 = SM2.generateKeyPair();
        int count = 100000;
        long start = System.currentTimeMillis();

        for(int i = 0; i < count; i++){
            HexBytes kp2pk = HexBytes.fromBytes(kp2.getPublicKey().getEncoded());
            byte[] s1 = cache.get(kp2pk, () -> CryptoContext.ecdh(true, kp1.getPrivateKey().getEncoded(), kp2.getPublicKey().getEncoded()));
        }
        long end = System.currentTimeMillis();
        System.out.println( (end - start) * 1.0 /(count));
    }

    @Test
    @Ignore
    public void test6(){
        HexBytes pk = HexBytes.fromHex("03DD5726AA2186FE1B0F03C31CBED058E8F8895E0FEB22991EF191EB944D38738E");
        HexBytes sk = HexBytes.fromHex("8AC8F54188960F0BDF4A949E7431CC552E35C16AA3C03739F1655AD8CDCDA363");
        HexBytes one = HexBytes.fromHex("0000000000000000000000000000000000000000000000000000000000000001");

        HexBytes expectedPk = HexBytes.fromBytes(new SM2PrivateKey(sk.getBytes())
                .generatePublicKey().getEncoded());

        HexBytes GX = HexBytes.fromBytes(new SM2PrivateKey(one.getBytes())
                .generatePublicKey().getEncoded());

        System.out.println(pk);
        System.out.println(expectedPk);

        System.out.println("gx = " + GX);
    }
}
