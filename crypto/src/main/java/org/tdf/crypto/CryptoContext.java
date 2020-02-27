package org.tdf.crypto;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.*;

import java.util.function.BiFunction;
import java.util.function.Function;

public class CryptoContext {
    public interface SignatureVerifier {
        boolean verify(byte[] pk, byte[] msg, byte[] sig);
    }

    public static Function<byte[], byte[]> hashFunction;

    // (pk, msg, sig) -> true/false
    public static SignatureVerifier signatureVerifier;

    // (sk, msg) -> signature
    public static BiFunction<byte[], byte[], byte[]> signer;

    // (sk, msg) -> encrypted
    public static BiFunction<byte[], byte[], byte[]> encrypt;

    // (sk, encrypted) -> msg
    public static BiFunction<byte[], byte[], byte[]> decrypt;

    // (alice's sk, bob's pk) -> key
    public static BiFunction<byte[], byte[], byte[]> ecdh;

    public static byte[] keccak256(byte[] in) {
        Digest digest = new KeccakDigest(256);
        return CryptoContext.hash(in, digest);
    }

    public static byte[] hash(byte[] input, Digest digest) {
        byte[] retValue = new byte[digest.getDigestSize()];
        digest.update(input, 0, input.length);
        digest.doFinal(retValue, 0);
        return retValue;
    }

    public static byte[] keccak512(byte[] in) {
        Digest digest = new KeccakDigest(512);
        return CryptoContext.hash(in, digest);
    }

    // TODO: configure digest according to -Dsunflower.crypto.hash
    public static byte[] digest(byte[] in) {
        return hashFunction.apply(in);
    }

    public static byte[] sha3256(byte[] in) {
        Digest digest = new SHA3Digest(256);
        return CryptoContext.hash(in, digest);
    }

    public static byte[] ripemd128(byte[] bytes) {
        Digest digest = new RIPEMD128Digest();
        digest.update(bytes, 0, bytes.length);
        byte[] rsData = new byte[digest.getDigestSize()];
        digest.doFinal(rsData, 0);
        return rsData;
    }

    public static byte[] ripemd160(byte[] bytes) {
        Digest digest = new RIPEMD160Digest();
        digest.update(bytes, 0, bytes.length);
        byte[] rsData = new byte[digest.getDigestSize()];
        digest.doFinal(rsData, 0);
        return rsData;
    }

    public static byte[] ripemd256(byte[] bytes) {
        Digest digest = new RIPEMD256Digest();
        digest.update(bytes, 0, bytes.length);
        byte[] rsData = new byte[digest.getDigestSize()];
        digest.doFinal(rsData, 0);
        return rsData;
    }

    public static byte[] ripemd320(byte[] bytes) {
        Digest digest = new RIPEMD320Digest();
        digest.update(bytes, 0, bytes.length);
        byte[] rsData = new byte[digest.getDigestSize()];
        digest.doFinal(rsData, 0);
        return rsData;
    }
}