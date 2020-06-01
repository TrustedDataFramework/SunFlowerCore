package org.tdf.sunflower.types;

import java.util.function.Function;

public class CryptoContext {
    private static Function<byte[], byte[]> hashFunction
            = Function.identity();

    private static SignatureVerifier signatureVerifier = (pk, msg, sig) -> true;

    public static void setSignatureVerifier(SignatureVerifier signatureVerifier) {
        CryptoContext.signatureVerifier = signatureVerifier;
    }

    private static int publicKeySize;

    public static void setPublicKeySize(int publicKeySize) {
        CryptoContext.publicKeySize = publicKeySize;
    }

    public static int getPublicKeySize() {
        return publicKeySize;
    }

    public static void setHashFunction(Function<byte[], byte[]> hashFunction) {
        CryptoContext.hashFunction = hashFunction;
    }

    public static byte[] hash(byte[] data) {
        return hashFunction.apply(data);
    }

    public static boolean verify(byte[] pk, byte[] msg, byte[] sig) {
        return CryptoContext.verify(pk, msg, sig);
    }
}
