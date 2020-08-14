package org.tdf.crypto.sm2;

import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.tdf.crypto.PrivateKey;
import org.tdf.gmhelper.SM2Util;


public class SM2PrivateKey implements PrivateKey {
    public SM2PrivateKey(BCECPrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public SM2PrivateKey(byte[] encoded) {
        this.privateKey = SM2Util.createBCECPrivateKey(encoded);
    }

    public BCECPrivateKey getPrivateKey() {
        return privateKey;
    }

    private BCECPrivateKey privateKey;

    public byte[] sign(byte[] msg) {
        byte[] signature = null;
        try {
            signature = SM2Util.decodeDERSM2Sign(SM2Util.sign(privateKey, msg));
        } catch (CryptoException e) {
            e.printStackTrace();
        }
        return signature;
    }

    public byte[] decrypt(byte[] cipher) {
        return SM2Util.decrypt(getEncoded(), cipher);
    }

    @Override
    public SM2PublicKey generatePublicKey() {
        return new SM2PublicKey(SM2Util.getBCECPublicKeyFromPrivateKey(getEncoded()));
    }

    @Override
    public String getAlgorithm() {
        return SM2.getAlgorithm();
    }

    @Override
    public String getFormat() {
        return "";
    }

    @Override
    public byte[] getEncoded() {
        return SM2Util.getRawPrivateKey(privateKey);
    }
}
