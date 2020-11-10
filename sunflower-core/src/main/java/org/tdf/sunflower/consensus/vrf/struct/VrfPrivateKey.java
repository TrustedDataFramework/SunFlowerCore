package org.tdf.sunflower.consensus.vrf.struct;

import org.tdf.crypto.CryptoException;
import org.tdf.crypto.PrivateKey;
import org.tdf.crypto.ed25519.Ed25519;
import org.tdf.sunflower.consensus.vrf.HashUtil;

public class VrfPrivateKey {
    private PrivateKey signer;

    public VrfPrivateKey(String algorithm) throws CryptoException {
        if (algorithm.equals(Ed25519.getAlgorithm())) {
            this.signer = Ed25519.generateKeyPair().getPrivateKey();
            return;
        }

//        if (algorithm.equals(Secp256k1.getAlgorithm())) {
//            this.signer = Secp256k1.GenerateKeyPair().getPrivateKey();
//            return;
//        }

        throw new CryptoException("unsupported signature policy");
    }

    public VrfPrivateKey(PrivateKey signer) {
        this.signer = signer;
    }

    /**
     * @param seed random seed
     * @return verifiable random function result,
     * consists of a random variable, the seed and a proof for verifying
     * @throws CryptoException
     */
    public VrfResult rand(byte[] seed) throws CryptoException {
        if (seed.length > 255) {
            throw new CryptoException("seed length overflow");
        }

        byte[] sign = signer.sign(seed);
        byte[] random = HashUtil.sha256(sign);
        return new VrfResult(random, sign);
    }

    public byte[] getEncoded() {
        return this.signer.getEncoded();
    }

    public VrfPublicKey generatePublicKey() {
        return new VrfPublicKey(this.signer.generatePublicKey());
    }

    public PrivateKey getSigner() {
        return this.signer;
    }
}