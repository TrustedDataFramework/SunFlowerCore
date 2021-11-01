package org.tdf.natives;

public class Crypto {
    public static native byte[] sm3(byte[] data);
    public static native byte[] sm2PkFromSk(byte[] privateKey, boolean compress);
    public static native boolean sm2Verify(long seed, byte[] message, byte[] publicKey, byte[] sig);
    public static native byte[] sm2Sign(long seed, byte[] privateKey, byte[] message);


    public static native byte[] mlsagVerify(long seed, byte[] message, byte[][] decoys, byte[] challenge, byte[][] responses, byte[][] keyImages);

    /**
     *
     * @param seed
     * @return {private_key: byte32, public_key: {x, y}, round_1: {p0, p1}}
     */
    public static native String schnorrGenSigner(long seed);

    /**
     *
     * @param index
     * @param sk bytes32
     * @param msg bytes
     * @param pubs {x: bytes32, y: bytes32}
     * @param r1s  {p0: {x, y}, p1: {x,y} }
     * @return
     */
    public static native String schnorrRound1(int index, byte[] sk, byte[] msg, String pubs, String r1s);

    /**
     *
     * @param prime bytes32
     * @param r  {x: bytes32, y: bytes32 }
     * @param rs2 bytes32[]
     * @return bytes32
     */
    public static native byte[] schnorrRound2(byte[] prime, String r, byte[][] rs2);

    /**
     *
     * @param sig bytes32
     * @param r {x, y}
     * @param c  bytes32
     * @param xtlide {x, y}
     * @return
     */
    public static native boolean schnorrVerify(byte[] sig, String r, byte[] c, String xtlide);

    static {
        JNIUtil.loadLibrary("chain_natives");
    }
}
