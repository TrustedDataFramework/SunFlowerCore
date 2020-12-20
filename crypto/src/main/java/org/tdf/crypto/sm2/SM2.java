package org.tdf.crypto.sm2;

import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.tdf.crypto.KeyPair;
import org.tdf.gmhelper.BCECUtil;
import org.tdf.gmhelper.SM2KeyExchangeUtil;
import org.tdf.gmhelper.SM2Util;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class SM2 {
    public static final String ALGORITHM = "sm2";
    public static int KEY_BITS = 16;//密钥协商生成的Key密钥长度，可以修改

    public static String getAlgorithm() {
        return ALGORITHM;
    }

    /**
     * @return SM2 keypair for signature and verifying
     */
    public static KeyPair generateKeyPair() {
        java.security.KeyPair keyPair = null;
        try {
            try {
                keyPair = SM2Util.generateKeyPair();
            } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
                e.printStackTrace();
            }
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
        return new SM2KeyPair(keyPair);
    }

    /**
     * 密钥协商功能
     *
     * @param initiator       是否密钥协商发起者
     * @param privStatic      本方固定私钥
     * @param privEphemeral   本方临时私钥
     * @param publicKeyStatic 对方固定公钥 压缩的33字节公钥
     * @param publicEphemeral 对方临时公钥 压缩的33字节公钥
     * @param userId          对方指定用户ID，注意本方ID为默认 "userid@soie-chain.com".getBytes()
     */
    public static byte[] calculateShareKey(boolean initiator, byte[] privStatic, byte[] privEphemeral, byte[] publicKeyStatic, byte[] publicEphemeral, byte[] userId) {
        SM2PrivateKey skStatic = new SM2PrivateKey(privStatic);
        SM2PrivateKey skEphemeral = new SM2PrivateKey(privEphemeral);
        ECPrivateKeyParameters skpStatic = BCECUtil.convertPrivateKeyToParameters(skStatic.getPrivateKey());
        ECPrivateKeyParameters skpEphemeral = BCECUtil.convertPrivateKeyToParameters(skEphemeral.getPrivateKey());
        SM2PublicKey pkStatic = new SM2PublicKey(publicKeyStatic);
        SM2PublicKey pkEphemeral = new SM2PublicKey(publicEphemeral);
        ECPublicKeyParameters pkpStatic = BCECUtil.convertPublicKeyToParameters(pkStatic.getBcecPublicKey());
        ECPublicKeyParameters pkpEphemeral = BCECUtil.convertPublicKeyToParameters(pkEphemeral.getBcecPublicKey());
        return SM2KeyExchangeUtil.calculateKey(initiator, KEY_BITS, skpStatic, skpEphemeral, SM2Util.WITH_ID,
                pkpStatic, pkpEphemeral, userId);

    }
}
