package org.tdf.crypto;

public interface PublicKey extends java.security.PublicKey {
    boolean verify(byte[] msg, byte[] signature);
}