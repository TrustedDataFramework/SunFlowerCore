package org.tdf.crypto;

public interface PrivateKey extends java.security.PrivateKey {
    byte[] sign(byte[] msg) throws CryptoException;

    PublicKey generatePublicKey();
}