package org.tdf.sunflower.types;

import org.tdf.common.serialize.Codec;
import org.tdf.common.store.ByteArrayMapStore;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;

import java.util.function.BiFunction;
import java.util.function.Function;

public class CryptoContext {
    private static Function<byte[], byte[]> hashFunction
            = Function.identity();

    private static SignatureVerifier signatureVerifier = (pk, msg, sig) -> true;

    // (sk, msg) -> signature
    public static BiFunction<byte[], byte[], byte[]> signer = (a, b) -> HexBytes.EMPTY_BYTES;

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

    private static Function<byte[], byte[]> getPkFromSk = Function.identity();

    public static void setGetPkFromSk(Function<byte[], byte[]> getPkFromSk) {
        CryptoContext.getPkFromSk = getPkFromSk;
    }

    public static byte[] getPkFromSk(byte[] sk) {
        return getPkFromSk.apply(sk);
    }

    public static byte[] getEmptyTrieRoot(Function<byte[], byte[]> hashFunction) {
        Trie<?, ?> trie = Trie.<byte[], byte[]>builder()
                .keyCodec(Codec.identity())
                .valueCodec(Codec.identity())
                .store(new ByteArrayMapStore<>())
                .hashFunction(hashFunction)
                .build();

        return trie.getNullHash();
    }

    public static  void setSigner(BiFunction<byte[], byte[], byte[]> signer){
        CryptoContext.signer = signer;
    }

    public static byte[] sign(byte[] sk, byte[] msg) {
        return signer.apply(sk, msg);
    }
}
