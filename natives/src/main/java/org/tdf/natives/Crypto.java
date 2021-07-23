package org.tdf.natives;

public class Crypto {
    public static native byte[] sm3(byte[] data);
    public static native byte[] sm2PkFromSk(byte[] privateKey, boolean compress);
    public static native boolean sm2Verify(long seed, byte[] message, byte[] publicKey, byte[] sig);
    public static native byte[] sm2Sign(long seed, byte[] privateKey, byte[] message);

    /**
     * generate mlsag private key
     * @param seed random seed
     * @return generated private key
     */
    public static native byte[] mlsagGetSk(long seed);

    /**
     *
     * @param privateKey
     * @param compress
     * @return
     */
    public static native byte[] mlsagPkFromSk(byte[] privateKey, boolean compress);

    public static native byte[] mlsagSign(byte[][] privateKeys, byte[] message);

    public static native byte[] mlsagVerify(byte[] message, byte[][] publicKeys, byte[] sig);

//    static {
//        JNIUtil.loadLibrary("chain_natives");
//    }
}
