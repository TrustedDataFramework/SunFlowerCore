package org.tdf.crypto;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.*;
import org.tdf.crypto.ed25519.Ed25519PublicKey;
import org.tdf.crypto.sm2.SM2;
import org.tdf.crypto.sm2.SM2PrivateKey;
import org.tdf.crypto.sm2.SM2PublicKey;
import org.tdf.gmhelper.SM3Util;
import org.tdf.gmhelper.SM4Util;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class CryptoContext {
    public interface SignatureVerifier {
        boolean verify(byte[] pk, byte[] msg, byte[] sig);
    }

    public static Function<byte[], byte[]> hashFunction = SM3Util::hash;

    // (pk, msg, sig) -> true/false
    public static SignatureVerifier signatureVerifier = (pk, msg, sig) -> new SM2PublicKey(pk).verify(msg, sig);

    // (sk, msg) -> signature
    public static BiFunction<byte[], byte[], byte[]> signer = (sk, msg) -> new SM2PrivateKey(sk).sign(msg);

    // (sk, msg) -> encrypted
    public static BiFunction<byte[], byte[], byte[]> encrypt;

    // (sk, encrypted) -> msg
    public static BiFunction<byte[], byte[], byte[]> decrypt;

    // (alice's sk, bob's pk) -> key
    public static BiFunction<byte[], byte[], byte[]> ecdh;

    // (sk) -> pk
    public static Function<byte[], byte[]> getPkFromSk = (sk) -> new SM2PrivateKey(sk).generatePublicKey().getEncoded();

    public static byte[] getPkFromSk(byte[] sk){
        return getPkFromSk.apply(sk);
    }

    public static Supplier<KeyPair> generateKeyPair = SM2::generateKeyPair;

    public static KeyPair generateKeyPair(){
        return generateKeyPair.get();
    }

    public static boolean verifySignature(byte[] pk, byte[] msg, byte[] sig) {
        return signatureVerifier.verify(pk, msg, sig);
    }

    public static byte[] ecdh(byte[] sk, byte[] pk) {
        return ecdh.apply(sk, pk);
    }

    public static byte[] sign(byte[] sk, byte[] msg) {
        return signer.apply(sk, msg);
    }

    public static byte[] encrypt(byte[] sk, byte[] msg) {
        return encrypt.apply(sk, msg);
    }

    public static byte[] decrypt(byte[] sk, byte[] encrypted) {
        return decrypt.apply(sk, encrypted);
    }

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