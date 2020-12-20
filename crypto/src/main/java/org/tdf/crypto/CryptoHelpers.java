package org.tdf.crypto;


import lombok.Data;
import lombok.NonNull;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.*;
import org.tdf.common.util.BigEndian;
import org.tdf.common.util.HexBytes;
import org.tdf.gmhelper.SM4Util;

import java.util.function.BiFunction;


public class CryptoHelpers {
    // (sk, msg) -> encrypted
    public static final BiFunction<byte[], byte[], byte[]> ENCRYPT = (key, msg) -> {
        try {
            return SM4Util.encrypt_Ecb_NoPadding(key, fill(msg));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    };


//
//    @SneakyThrows
//    public static byte[] ecdh(boolean initiator, byte[] sk, byte[] pk) {
//        return cache.get(new ECDHParameters(initiator, sk, pk), () -> ecdhInternal(initiator, sk, pk));
//    }

    //    public static Ecdh ecdh = (initiator, sk, pk) ->
//            SM2.calculateShareKey(initiator, sk, sk, pk, pk, SM2Util.WITH_ID);
    // (sk, encrypted) -> msg
    public static final BiFunction<byte[], byte[], byte[]> DECRYPT = (key, encryptMsg) -> {
        try {
            return restore(SM4Util.decrypt_Ecb_NoPadding(key, encryptMsg));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    };

    // len + msg + (0x00...)
    public static byte[] fill(byte[] msg) {
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

    public static byte[] keccak256(byte[] in) {
        Digest digest = new KeccakDigest(256);
        return CryptoHelpers.hash(in, digest);
    }

//
//    private static byte[] ecdhInternal(boolean initiator, byte[] sk, byte[] pk) {
//        return ecdh.exchange(initiator, sk, pk);
//    }

    private static byte[] hash(byte[] input, Digest digest) {
        byte[] retValue = new byte[digest.getDigestSize()];
        digest.update(input, 0, input.length);
        digest.doFinal(retValue, 0);
        return retValue;
    }

    public static byte[] keccak512(byte[] in) {
        Digest digest = new KeccakDigest(512);
        return CryptoHelpers.hash(in, digest);
    }

    public static byte[] sha3256(byte[] in) {
        Digest digest = new SHA3Digest(256);
        return CryptoHelpers.hash(in, digest);
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

}
