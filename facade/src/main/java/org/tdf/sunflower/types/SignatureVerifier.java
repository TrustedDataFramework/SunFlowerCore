package org.tdf.sunflower.types;

public interface SignatureVerifier {
    boolean verify(byte[] pk, byte[] msg, byte[] sig);
}
