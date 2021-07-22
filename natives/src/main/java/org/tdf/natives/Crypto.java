package org.tdf.natives;

public class Crypto {
    public static native byte[] sm3(byte[] data) throws Error;
    public static native byte[] sm2PkFromSk(byte[] privateKey, boolean compress) throws Error;
    public static native boolean sm2Verify(long seed, byte[] message, byte[] publicKey, byte[] sig) throws Error;
    public static native byte[] sm2Sign(long seed, byte[] privateKey, byte[] message) throws Error;

    static {
        JNIUtil.loadLibrary("chain_natives");
    }
}
