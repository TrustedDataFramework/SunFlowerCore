package org.tdf.crypto;

public interface KeyPair{
    PrivateKey getPrivateKey();
    PublicKey getPublicKey();
}