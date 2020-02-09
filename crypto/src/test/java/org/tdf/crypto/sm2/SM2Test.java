package org.tdf.crypto.sm2;


import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;

import org.junit.Test;
import static org.junit.Assert.*;
import org.tdf.crypto.KeyPair;
import org.tdf.crypto.sm2.SM2;
import org.tdf.crypto.sm2.SM2PrivateKey;
import org.tdf.crypto.sm2.SM2PublicKey;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class SM2Test {

    @Test
    public void testSingle() {// 测试创建单个密钥对、签名、验签
        KeyPair keyPair = SM2.generateKeyPair();
        SM2PrivateKey sm2PrivateKey = (SM2PrivateKey) keyPair.getPrivateKey();
        SM2PublicKey sm2PublicKey = (SM2PublicKey) keyPair.getPublicKey();
        byte[] signature = sm2PrivateKey.sign("sm2 test".getBytes());

        System.out.println("Private key: " + ByteUtils.toHexString(sm2PrivateKey.getEncoded()) + " len:" + sm2PrivateKey.getEncoded().length);
        System.out.println("Public key: " + ByteUtils.toHexString(sm2PublicKey.getEncoded()) + " len:" + sm2PublicKey.getEncoded().length);
        System.out.println("Signature: " + ByteUtils.toHexString(signature) + " len:" + signature.length);
        assertTrue(sm2PublicKey.verify("sm2 test".getBytes(), signature));
    }

    @Test
    public void testCreatePrivateKey() {// 测试从字节数组形式的私钥创建私钥对象
        SM2PrivateKey sm2PrivateKey = new SM2PrivateKey(ByteUtils.fromHexString("74a7d0f0520366b09c16296aaec44102d1073b44fda1c25561a242228d75c863"));

        SM2PublicKey sm2PublicKey = (SM2PublicKey)sm2PrivateKey.generatePublicKey();
        byte[] signature = sm2PrivateKey.sign("sm2 test".getBytes());
        assertTrue(sm2PublicKey.verify("sm2 test".getBytes(), signature));
    }

    @Test
    public void testCreatePublicKey() { // 测试从字节数组形式的公钥创建公钥对象
        SM2PrivateKey sm2PrivateKey = new SM2PrivateKey(ByteUtils.fromHexString("f79bfd80ff249b35622669833844c6117f3a32dbdb18f5ad1a844389d3a3d2d4"));
        SM2PublicKey sm2PublicKey = new SM2PublicKey(ByteUtils.fromHexString("035eef0e68008d1abd45cde6c68f471d901e083a0c010072cab4214439b4914b5b"));
        byte[] signature = sm2PrivateKey.sign("sm2 test".getBytes());
        assertTrue( sm2PublicKey.verify("sm2 test".getBytes(), signature));
    }
    @Test
    public void testAll() {
        String msg = "sm2 test";
        KeyPair keyPair = SM2.generateKeyPair();
        SM2PrivateKey sm2PrivateKey0 = (SM2PrivateKey) keyPair.getPrivateKey();
        byte[] priv = sm2PrivateKey0.getEncoded();
        byte[] pub = keyPair.getPublicKey().getEncoded();
        SM2PrivateKey sm2PrivateKey1 = new SM2PrivateKey(priv);

        SM2PublicKey sm2PublicKey0 = new SM2PublicKey(pub);
        SM2PublicKey sm2PublicKey1 = (SM2PublicKey)sm2PrivateKey0.generatePublicKey();
        SM2PublicKey sm2PublicKey2 = (SM2PublicKey)sm2PrivateKey1.generatePublicKey();
        byte[] signature1 = sm2PrivateKey1.sign(msg.getBytes());
        byte[] signature0 = sm2PrivateKey0.sign(msg.getBytes());

        assertTrue(sm2PublicKey0.verify(msg.getBytes(), signature0)  &&
                sm2PublicKey0.verify(msg.getBytes(), signature1) &&
                sm2PublicKey1.verify(msg.getBytes(), signature0) &&
                sm2PublicKey1.verify(msg.getBytes(), signature1) &&
                sm2PublicKey2.verify(msg.getBytes(), signature0) &&
                sm2PublicKey2.verify(msg.getBytes(), signature1));
    }

    @Test
    public void testEncryptDecrypt() {
        String msg = "sm2 test";
        KeyPair keyPair = SM2.generateKeyPair();
        SM2PublicKey sm2PublicKey = (SM2PublicKey) keyPair.getPublicKey();
        SM2PrivateKey sm2PrivateKey = (SM2PrivateKey) keyPair.getPrivateKey();
        byte[] cipher = sm2PublicKey.encrypt(msg.getBytes());
        byte[] result = sm2PrivateKey.decrypt(cipher);
        System.out.println(ByteUtils.toHexString(sm2PrivateKey.getEncoded()));
        System.out.println(ByteUtils.toHexString(sm2PublicKey.getBcecPublicKey().getQ().getEncoded(false)));
        System.out.println(ByteUtils.toHexString(cipher));
        System.out.println(new String(result));
        assertArrayEquals(msg.getBytes(), result);
    }

    @Test
    public void testDecrypt() throws UnsupportedEncodingException {
        SM2PrivateKey sm2PrivateKey = new SM2PrivateKey(ByteUtils.fromHexString("e0ef317fc7c71279ec4a9843597c245f6564edeb89e238e35c5b7ec782947435"));
        byte[] result = sm2PrivateKey.decrypt(ByteUtils.fromHexString("04dff5529c98cad70e22e1ee0de12b1bba38557ca6735a045d7b08aeeb7c9f8006acc083ad9c19e400b1f4149f52e4f90da4fd747067074608f3d357c1770178b3682db6cd74d968122419d751d6edc5daf6dc72d203c8d7eb7c8e2ed4ccf3e921ad2126554851"));
        String plaintext = new String(result, StandardCharsets.UTF_8);
        System.out.println(plaintext);
    }

    @Test
    public void testExchange() {
        KeyPair keyPair0 = SM2.generateKeyPair();
        KeyPair keyPair1 = SM2.generateKeyPair();

        KeyPair keyPair2 = SM2.generateKeyPair();
        KeyPair keyPair3 = SM2.generateKeyPair();

        byte[] privateKey0 = keyPair0.getPrivateKey().getEncoded();
        byte[] privateKey1 = keyPair1.getPrivateKey().getEncoded();
        byte[] privateKey2 = keyPair2.getPrivateKey().getEncoded();
        byte[] privateKey3 = keyPair3.getPrivateKey().getEncoded();
        byte[] pubkey0 = keyPair0.getPublicKey().getEncoded();
        byte[] pubkey1 = keyPair1.getPublicKey().getEncoded();
        byte[] pubkey2 = keyPair2.getPublicKey().getEncoded();
        byte[] pubkey3 = keyPair3.getPublicKey().getEncoded();

        byte[] key0 = SM2.calculateShareKey(true, privateKey0,privateKey1,pubkey2,pubkey3, "userid@soie-chain.com".getBytes());
        byte[] key1 = SM2.calculateShareKey(false, privateKey2,privateKey3,pubkey0,pubkey1, "userid@soie-chain.com".getBytes());

        assertArrayEquals(key0, key1);

    }
}