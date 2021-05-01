package org.tdf.sunflower.types;

import lombok.Setter;
import org.tdf.common.serialize.Codec;
import org.tdf.common.store.ByteArrayMapStore;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.facade.ECDH;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class CryptoContext {
    // (sk, msg) -> signature
    @Setter
    public static BiFunction<byte[], byte[], byte[]> signer = (a, b) -> HexBytes.EMPTY_BYTES;
    @Setter
    private static Function<byte[], byte[]> hashFunction
            = Function.identity();
    @Setter
    private static SignatureVerifier signatureVerifier = (pk, msg, sig) -> true;
    @Setter
    private static Supplier<byte[]> secretKeyGenerator = () -> HexBytes.EMPTY_BYTES;

    @Setter
    private static int publicKeySize;

    @Setter
    private static Function<byte[], byte[]> getPkFromSk = Function.identity();

    // (key, plain) -> cipher
    @Setter
    private static BiFunction<byte[], byte[], byte[]> encrypt = (x, y) -> y;

    // (key, cipher) -> plain
    @Setter
    private static BiFunction<byte[], byte[], byte[]> decrypt = (x, y) -> y;

    @Setter
    private static ECDH ecdh = (x, y, z) -> HexBytes.EMPTY_BYTES;

    public static int getPublicKeySize() {
        return publicKeySize;
    }

    public static byte[] hash(byte[] data) {
        return hashFunction.apply(data);
    }

    public static boolean verify(byte[] pk, byte[] msg, byte[] sig) {
        return signatureVerifier.verify(pk, msg, sig);
    }

    public static byte[] getPkFromSk(byte[] sk) {
        return getPkFromSk.apply(sk);
    }

    public static HexBytes getEmptyTrieRoot() {
        Trie<?, ?> trie = Trie.<byte[], byte[]>builder()
                .keyCodec(Codec.identity())
                .valueCodec(Codec.identity())
                .store(new ByteArrayMapStore<>())
                .hashFunction(hashFunction)
                .build();

        return trie.getNullHash();
    }

    public static byte[] generateSecretKey() {
        return secretKeyGenerator.get();
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


    public static byte[] ecdh(boolean initiator, byte[] sk, byte[] pk) {
        return ecdh.exchange(initiator, sk, pk);
    }

    public static byte[] ecdh(byte[] sk, byte[] pk) {
        return ecdh.exchange(sk, pk);
    }

    public static Function<byte[], byte[]> keccak256 = null;

    public static byte[] keccak256(byte[] in) {
        return keccak256.apply(in);
    }
}
