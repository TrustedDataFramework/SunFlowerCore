package org.tdf.crypto.ed25519;

import org.bouncycastle.crypto.Signer;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.tdf.crypto.CryptoException;
import org.tdf.crypto.PrivateKey;


public class Ed25519PrivateKey implements PrivateKey {

    private static final long serialVersionUID = -2900765828422070500L;

    private Ed25519PrivateKeyParameters privateKey;

    public Ed25519PrivateKey(byte[] encoded) {
        this.privateKey = new Ed25519PrivateKeyParameters(encoded, 0);
    }

    Ed25519PrivateKey(Ed25519PrivateKeyParameters privateKey) {
        this.privateKey = privateKey;
    }

    /**
     * @param msg plain text
     * @return signature
     * @throws CryptoException
     */
    public byte[] sign(byte[] msg) throws CryptoException {
        try {
            Signer signer = new Ed25519Signer();
            signer.init(true, privateKey);
            signer.update(msg, 0, msg.length);
            return signer.generateSignature();
        } catch (Exception e) {
            throw new CryptoException();
        }
    }

    public String getAlgorithm() {
        return Ed25519.ALGORITHM;
    }

    public String getFormat() {
        return "";
    }

    public byte[] getEncoded() {
        return this.privateKey.getEncoded();
    }

    public Ed25519PublicKey generatePublicKey() {
        return new Ed25519PublicKey(this.privateKey.generatePublicKey());
    }

}
