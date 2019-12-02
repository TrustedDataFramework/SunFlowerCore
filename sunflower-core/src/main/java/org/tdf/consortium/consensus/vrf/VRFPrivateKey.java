package org.wisdom.consortium.consensus.vrf;

import org.wisdom.crypto.CryptoException;
import org.wisdom.crypto.PrivateKey;
import org.wisdom.crypto.ed25519.Ed25519;

public class VRFPrivateKey {
    private PrivateKey signer;

    public VRFPrivateKey(String algorithm) throws CryptoException{
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

    /**
     *
     * @param seed random seed
     * @return verifiable random function result,
     * consists of a random variable, the seed and a proof for verifying
     * @throws CryptoException
     */
    public VRFResult rand(byte[] seed) throws CryptoException {
        if (seed.length > 255) {
            throw new CryptoException("seed length overflow");
        }

        byte[] sign = signer.sign(seed);
        byte[] random = HashUtil.sha256(sign);
        return new VRFResult(random, sign);
    }

    public VRFPrivateKey(PrivateKey signer) {
        this.signer = signer;
    }
    public byte[] getEncoded(){
        return this.signer.getEncoded();
    }
    public VRFPublicKey generatePublicKey(){
        return new VRFPublicKey(this.signer.generatePublicKey());
    }

    public PrivateKey getSigner() {
        return this.signer;
    }
}