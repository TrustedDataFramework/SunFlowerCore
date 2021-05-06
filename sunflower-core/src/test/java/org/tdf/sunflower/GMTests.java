package org.tdf.sunflower;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Hex;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.util.FastByteComparisons;
import org.tdf.common.util.HexBytes;
import org.tdf.crypto.CryptoHelpers;
import org.tdf.crypto.KeyPair;
import org.tdf.crypto.sm2.SM2;
import org.tdf.crypto.sm2.SM2PrivateKey;
import org.tdf.crypto.sm2.SM2PublicKey;
import org.tdf.gmhelper.SM2Util;
import org.tdf.gmhelper.SM3Util;
import org.tdf.sunflower.types.CryptoContext;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.PriorityQueue;

@RunWith(JUnit4.class)
public class GMTests {
    public static final int ADDRESS_SIZE = 20;

    static {
        CryptoContext.setSignatureVerifier((pk, msg, sig) -> new SM2PublicKey(pk).verify(msg, sig));
        CryptoContext.setSigner((sk, msg) -> new SM2PrivateKey(sk).sign(msg));
        CryptoContext.setSecretKeyGenerator(() -> SM2.generateKeyPair().getPrivateKey().getEncoded());
        CryptoContext.setGetPkFromSk((sk) -> new SM2PrivateKey(sk).generatePublicKey().getEncoded());
        CryptoContext.setEcdh((initiator, sk, pk) -> SM2.calculateShareKey(initiator, sk, sk, pk, pk, SM2Util.WITH_ID));
        CryptoContext.setEncrypt(CryptoHelpers.ENCRYPT);
        CryptoContext.setDecrypt(CryptoHelpers.DECRYPT);
    }

    @Test
    public void test() {
        KeyPair keyPair = SM2.generateKeyPair();
        System.out.println(keyPair.getPublicKey().getEncoded().length);
    }

    @Test
    public void test1() {
        PriorityQueue<Long> q = new PriorityQueue<>(Long::compare);
        q.add(1L);
        q.add(1L);
        assert q.size() == 2;
    }

    @Test
    @Ignore
    public void test3() {
        for (int i = 0; i < 7; i++) {
            KeyPair kp = SM2.generateKeyPair();
            String sk = Hex.encodeHexString(kp.getPrivateKey().getEncoded());
            String pk = Hex.encodeHexString(kp.getPublicKey().getEncoded());
            byte[] pkhash = SM3Util.hash(kp.getPublicKey().getEncoded());
            String address = Hex.encodeHexString(Arrays.copyOfRange(pkhash, pkhash.length - 20, pkhash.length));
            System.out.println(
                String.format("accounts sk: %s pk: %s address %s", sk, pk, address)
            );
        }
    }

    @Test
    @Ignore
    public void test4() {
        KeyPair kp1 = SM2.generateKeyPair();
        KeyPair kp2 = SM2.generateKeyPair();
        int count = 100000;
        long start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            byte[] s1 = CryptoContext.ecdh(true, kp1.getPrivateKey().getEncoded(), kp2.getPublicKey().getEncoded());
        }
        long end = System.currentTimeMillis();
        System.out.println((end - start) * 1.0 / (count));
    }

    @Test
    @SneakyThrows
//    @Ignore
    public void test5() {
        Cache<HexBytes, byte[]> cache = CacheBuilder.newBuilder()
            .maximumSize(1024)
            .build();

        KeyPair kp1 = SM2.generateKeyPair();
        KeyPair kp2 = SM2.generateKeyPair();
        int count = 100000;
        long start = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            HexBytes kp2pk = HexBytes.fromBytes(kp2.getPublicKey().getEncoded());
            byte[] s1 = cache.get(kp2pk, () -> CryptoContext.ecdh(true, kp1.getPrivateKey().getEncoded(), kp2.getPublicKey().getEncoded()));
        }
        long end = System.currentTimeMillis();
        System.out.println((end - start) * 1.0 / (count));
    }

    @Test
//    @Ignore
    public void test6() {
        HexBytes pk = HexBytes.fromHex("03C5D05CB953CD842CECE23DB312688C17D4863F73F678961F7DFBD328DBA509E9");
        HexBytes sk = HexBytes.fromHex("49CC13189AE10A60AA757169AEAEC491F4F6932D67E4E5D02D9889A6E244ACED");
        HexBytes one = HexBytes.fromHex("0000000000000000000000000000000000000000000000000000000000000001");

        HexBytes expectedPk = HexBytes.fromBytes(new SM2PrivateKey(sk.getBytes())
            .generatePublicKey().getEncoded());

        HexBytes GX = HexBytes.fromBytes(new SM2PrivateKey(one.getBytes())
            .generatePublicKey().getEncoded());

        System.out.println(pk);
        System.out.println(expectedPk);

        HexBytes ret = HexBytes.fromBytes(SM3Util.hash(pk.getBytes()));
        ret = ret.slice(ret.size() - ADDRESS_SIZE, ret.size());
        System.out.println("gx = " + GX);
        System.out.println("address = " + ret);
        System.out.println(HexBytes.fromBytes(SM3Util.hash(new byte[32])));
    }

    @Test
    public void testECDH() {
        byte[] aliceSk = CryptoContext.generateSecretKey();
        byte[] alicePk = CryptoContext.getPkFromSk(aliceSk);
        byte[] bobSk = CryptoContext.generateSecretKey();
        byte[] bobPk = CryptoContext.getPkFromSk(bobSk);

        byte[] k1 = CryptoContext.ecdh(true, aliceSk, bobPk);
        byte[] k2 = CryptoContext.ecdh(false, bobSk, alicePk);

        assert FastByteComparisons.equal(k1, k2);

        byte[] plain = "123abc".getBytes(StandardCharsets.US_ASCII);
        byte[] cipher = CryptoContext.encrypt(k1, plain);
        byte[] plain2 = CryptoContext.decrypt(k1, cipher);

        System.out.println(HexBytes.fromBytes(plain));
        System.out.println(HexBytes.fromBytes(plain2));

    }
}
