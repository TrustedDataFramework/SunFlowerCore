package org.tdf.sunflower.crypto;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.*;
import org.tdf.common.util.BigEndian;
import org.tdf.common.util.HexBytes;
import org.tdf.crypto.KeyPair;
import org.tdf.crypto.sm2.SM2;
import org.tdf.crypto.sm2.SM2PrivateKey;
import org.tdf.crypto.sm2.SM2PublicKey;
import org.tdf.gmhelper.SM2Util;
import org.tdf.gmhelper.SM3Util;
import org.tdf.gmhelper.SM4Util;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class CryptoContext {

    public interface SignatureVerifier {
        boolean verify(byte[] pk, byte[] msg, byte[] sig);
    }

    public interface Ecdh {
        byte[] exchange(boolean initiator, byte[] sk, byte[] pk);
    }

    public static Function<byte[], byte[]> hashFunction = SM3Util::hash;

    // (pk, msg, sig) -> true/false
    public static SignatureVerifier signatureVerifier = (pk, msg, sig) -> new SM2PublicKey(pk).verify(msg, sig);

    @Data
    private static class ECDHParameters {
        private boolean initiator;
        private HexBytes sk;
        private HexBytes pk;

        ECDHParameters(boolean initiator, @NonNull byte[] sk, @NonNull byte[] pk) {
            this.initiator = initiator;
            this.sk = HexBytes.fromBytes(sk);
            this.pk = HexBytes.fromBytes(pk);
        }
    }

    private static final Cache<ECDHParameters, byte[]> cache = CacheBuilder.newBuilder().maximumSize(1024)
            .build();

    @SneakyThrows
    public static byte[] ecdh(boolean initiator, byte[] sk, byte[] pk) {
        return cache.get(new ECDHParameters(initiator, sk, pk), () -> ecdhInternal(initiator, sk, pk));
    }

    public static Ecdh ecdh = (initiator, sk, pk) ->
            SM2.calculateShareKey(initiator, sk, sk, pk, pk, SM2Util.WITH_ID);

    // (sk, msg) -> signature
    public static BiFunction<byte[], byte[], byte[]> signer = (sk, msg) -> new SM2PrivateKey(sk).sign(msg);


    // (sk, msg) -> encrypted
    public static BiFunction<byte[], byte[], byte[]> encrypt = (key, msg) -> {
        try {
            return SM4Util.encrypt_Ecb_NoPadding(key, fill(msg));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    };

    // (sk, encrypted) -> msg
    public static BiFunction<byte[], byte[], byte[]> decrypt = (key, encryptMsg) -> {
        try {
            return restore(SM4Util.decrypt_Ecb_NoPadding(key, encryptMsg));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    };

    // len + msg + (0x00...)
    private static byte[] fill(byte[] msg) {
        byte[] len = BigEndian.encodeInt32(msg.length);
        byte[] tail = new byte[16 - (msg.length + len.length) % 16];
        byte[] ret = new byte[len.length + msg.length + tail.length];
        int pos = 0;
        System.arraycopy(len, 0, ret, pos, len.length);
        pos += len.length;
        System.arraycopy(msg, 0, ret, pos, msg.length);
        pos += msg.length;
        System.arraycopy(tail, 0, ret, pos, tail.length);
        return ret;
    }

    private static byte[] restore(byte[] msg) {
        byte[] len = new byte[4];
        System.arraycopy(msg, 0, len, 0, len.length);
        int length = BigEndian.decodeInt32(len);
        byte[] ret = new byte[length];
        System.arraycopy(msg, len.length, ret, 0, length);
        return ret;
    }

    // (sk) -> pk
    public static Function<byte[], byte[]> getPkFromSk = (sk) -> new SM2PrivateKey(sk).generatePublicKey().getEncoded();

    public static byte[] getPkFromSk(byte[] sk) {
        return getPkFromSk.apply(sk);
    }

    public static Supplier<KeyPair> generateKeyPair = SM2::generateKeyPair;

    public static KeyPair generateKeyPair() {
        return generateKeyPair.get();
    }

    public static boolean verifySignature(byte[] pk, byte[] msg, byte[] sig) {
        return signatureVerifier.verify(pk, msg, sig);
    }


    private static byte[] ecdhInternal(boolean initiator, byte[] sk, byte[] pk) {
        return ecdh.exchange(initiator, sk, pk);
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

    public static byte[] hash(byte[] input) {
        return digest(input);
    }

    private static byte[] hash(byte[] input, Digest digest) {
        byte[] retValue = new byte[digest.getDigestSize()];
        digest.update(input, 0, input.length);
        digest.doFinal(retValue, 0);
        return retValue;
    }

    public static byte[] keccak512(byte[] in) {
        Digest digest = new KeccakDigest(512);
        return CryptoContext.hash(in, digest);
    }

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