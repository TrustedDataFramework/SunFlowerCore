package org.tdf.crypto.sm2;

import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.tdf.crypto.KeyPair;
import org.tdf.crypto.PrivateKey;
import org.tdf.crypto.PublicKey;

public class SM2KeyPair implements KeyPair {
    private java.security.KeyPair keyPair;
    private SM2PrivateKey sm2PrivateKey;
    private SM2PublicKey sm2PublicKey;

    public SM2KeyPair(java.security.KeyPair keyPair) {
        this.keyPair = keyPair;
        sm2PrivateKey = new SM2PrivateKey((BCECPrivateKey) keyPair.getPrivate());
        sm2PublicKey = new SM2PublicKey((BCECPublicKey) keyPair.getPublic());
    }

    @Override
    public PrivateKey getPrivateKey() {
        return sm2PrivateKey;
    }

    @Override
    public PublicKey getPublicKey() {
        return sm2PublicKey;
    }
}
