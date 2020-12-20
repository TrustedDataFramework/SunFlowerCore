package org.tdf.gmhelper;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.crypto.params.*;
import org.bouncycastle.math.ec.ECAlgorithms;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.Arrays;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

/**
 * SM2 Key Exchange protocol - based on https://tools.ietf.org/html/draft-shen-sm2-ecdsa-02
 */
public class SM2KeyExchange {
    private final Digest digest;

    private byte[] userID;
    private ECPrivateKeyParameters staticKey;
    private ECPoint staticPubPoint;
    private ECPoint ephemeralPubPoint;
    private ECDomainParameters ecParams;
    private int w;
    private ECPrivateKeyParameters ephemeralKey;
    private boolean initiator;

    public SM2KeyExchange() {
        this(new SM3Digest());
    }

    public SM2KeyExchange(Digest digest) {
        this.digest = digest;
    }

    /**
     * 字节数组拼接
     *
     * @param params
     * @return
     */
    private static byte[] join(byte[]... params) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] res = null;
        try {
            for (int i = 0; i < params.length; i++) {
                baos.write(params[i]);
            }
            res = baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    public static byte[] toByteArray(int i) {
        byte[] byteArray = new byte[4];
        byteArray[0] = (byte) (i >>> 24);
        byteArray[1] = (byte) ((i & 0xFFFFFF) >>> 16);
        byteArray[2] = (byte) ((i & 0xFFFF) >>> 8);
        byteArray[3] = (byte) (i & 0xFF);
        return byteArray;
    }

    /**
     * 密钥派生函数
     *
     * @param digest 摘要函数
     * @param Z      input
     * @param klen   生成klen字节数长度的密钥
     */
    private static byte[] KDF(Digest digest, byte[] Z, int klen) {
        int ct = 1; // a)
        int end = (int) Math.ceil(klen * 1.0 / 32);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] last = new byte[digest.getDigestSize()];
        try {
            for (int i = 1; i < end; i++) {
                digest.update(Z, 0, Z.length);
                digest.update(toByteArray(ct), 0, toByteArray(ct).length);
                digest.doFinal(last, 0);
                baos.write(last);// b.1)
                ct++;//b.2)
            }
            digest.update(Z, 0, Z.length);
            digest.update(toByteArray(ct), 0, toByteArray(ct).length);
            digest.doFinal(last, 0);
            if (klen % 32 == 0) {
                baos.write(last);
            } else
                baos.write(last, 0, klen % 32);
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void init(
            CipherParameters privParam) {
        SM2KeyExchangePrivateParameters baseParam;

        if (privParam instanceof ParametersWithID) {
            baseParam = (SM2KeyExchangePrivateParameters) ((ParametersWithID) privParam).getParameters();
            userID = ((ParametersWithID) privParam).getID();
        } else {
            baseParam = (SM2KeyExchangePrivateParameters) privParam;
            userID = new byte[0];
        }

        initiator = baseParam.isInitiator();
        staticKey = baseParam.getStaticPrivateKey();
        ephemeralKey = baseParam.getEphemeralPrivateKey();
        ecParams = staticKey.getParameters();
        staticPubPoint = baseParam.getStaticPublicPoint();
        ephemeralPubPoint = baseParam.getEphemeralPublicPoint();

        w = ecParams.getCurve().getFieldSize() / 2 - 1;
    }

    public byte[] calculateKey(int kLen, CipherParameters pubParam) {
        SM2KeyExchangePublicParameters otherPub;
        byte[] otherUserID;

        if (pubParam instanceof ParametersWithID) {
            otherPub = (SM2KeyExchangePublicParameters) ((ParametersWithID) pubParam).getParameters();
            otherUserID = ((ParametersWithID) pubParam).getID();
        } else {
            otherPub = (SM2KeyExchangePublicParameters) pubParam;
            otherUserID = new byte[0];
        }

        byte[] za = getZ(digest, userID, staticPubPoint);
        byte[] zb = getZ(digest, otherUserID, otherPub.getStaticPublicKey().getQ());

        ECPoint U = calculateU(otherPub);

        byte[] rv;
        if (initiator) {
            rv = KDF(digest, join(U.getXCoord().getEncoded(),
                    U.getYCoord().getEncoded(), za, zb), kLen);
        } else {
            rv = KDF(digest, join(U.getXCoord().getEncoded(),
                    U.getYCoord().getEncoded(), zb, za), kLen);
        }

        return rv;
    }

    public byte[][] calculateKeyWithConfirmation(int kLen, byte[] confirmationTag, CipherParameters pubParam) {
        SM2KeyExchangePublicParameters otherPub;
        byte[] otherUserID;

        if (pubParam instanceof ParametersWithID) {
            otherPub = (SM2KeyExchangePublicParameters) ((ParametersWithID) pubParam).getParameters();
            otherUserID = ((ParametersWithID) pubParam).getID();
        } else {
            otherPub = (SM2KeyExchangePublicParameters) pubParam;
            otherUserID = new byte[0];
        }

        if (initiator && confirmationTag == null) {
            throw new IllegalArgumentException("if initiating, confirmationTag must be set");
        }

        byte[] za = getZ(digest, userID, staticPubPoint);
        byte[] zb = getZ(digest, otherUserID, otherPub.getStaticPublicKey().getQ());

        ECPoint U = calculateU(otherPub);

        byte[] rv;
        if (initiator) {
            rv = KDF(digest, join(U.getXCoord().getEncoded(),
                    U.getYCoord().getEncoded(), za, zb), kLen);
            // A9
            byte[] inner = calculateInnerHash(digest, U, za, zb, ephemeralPubPoint, otherPub.getEphemeralPublicKey().getQ());

            byte[] s1 = S1(digest, U, inner);

            if (!Arrays.constantTimeAreEqual(s1, confirmationTag)) {
                throw new IllegalStateException("confirmation tag mismatch");
            }
            // A10
            return new byte[][]{rv, S2(digest, U, inner)};
        } else {
            rv = KDF(digest, join(U.getXCoord().getEncoded(),
                    U.getYCoord().getEncoded(), zb, za), kLen);
            // B10
            byte[] inner = calculateInnerHash(digest, U, zb, za, otherPub.getEphemeralPublicKey().getQ(), ephemeralPubPoint);
            byte[] s2 = S2(digest, U, inner);
            if (!Arrays.constantTimeAreEqual(s2, confirmationTag)) {
                throw new IllegalStateException("confirmation tag mismatch");
            }
            return new byte[][]{rv, S1(digest, U, inner)};
        }
    }

    private ECPoint calculateU(SM2KeyExchangePublicParameters otherPub) {
        ECDomainParameters params = staticKey.getParameters();

        ECPoint pB = ECAlgorithms.cleanPoint(params.getCurve(), otherPub.getStaticPublicKey().getQ());
        ECPoint rB = ECAlgorithms.cleanPoint(params.getCurve(), otherPub.getEphemeralPublicKey().getQ());
        // A4
        BigInteger x1 = reduce(ephemeralPubPoint.getAffineXCoord().toBigInteger());
        // A5
        BigInteger tA = staticKey.getD().add(x1.multiply(ephemeralKey.getD())).mod(ecParams.getN());
        // A6
        BigInteger x2 = reduce(rB.getAffineXCoord().toBigInteger());

        BigInteger k1 = ecParams.getH().multiply(tA).mod(ecParams.getN());
        BigInteger k2 = k1.multiply(x2).mod(ecParams.getN());
        // A7
        return ECAlgorithms.sumOfTwoMultiplies(pB, k1, rB, k2).normalize();
    }

    //x1~=2^w+(x1 AND (2^w-1))
    private BigInteger reduce(BigInteger x) {
        return x.and(BigInteger.valueOf(1).shiftLeft(w).subtract(BigInteger.valueOf(1))).setBit(w);
    }

    private byte[] S1(Digest digest, ECPoint u, byte[] inner) {
        digest.update((byte) 0x02);
        addFieldElement(digest, u.getAffineYCoord());
        digest.update(inner, 0, inner.length);

        return digestDoFinal();
    }

    private byte[] calculateInnerHash(Digest digest, ECPoint u, byte[] za, byte[] zb, ECPoint p1, ECPoint p2) {
        addFieldElement(digest, u.getAffineXCoord());
        digest.update(za, 0, za.length);
        digest.update(zb, 0, zb.length);
        addFieldElement(digest, p1.getAffineXCoord());
        addFieldElement(digest, p1.getAffineYCoord());
        addFieldElement(digest, p2.getAffineXCoord());
        addFieldElement(digest, p2.getAffineYCoord());

        return digestDoFinal();
    }

    private byte[] S2(Digest digest, ECPoint u, byte[] inner) {
        digest.update((byte) 0x03);
        addFieldElement(digest, u.getAffineYCoord());
        digest.update(inner, 0, inner.length);

        return digestDoFinal();
    }

    private byte[] getZ(Digest digest, byte[] userID, ECPoint pubPoint) {
        addUserID(digest, userID);
        addFieldElement(digest, ecParams.getCurve().getA());
        addFieldElement(digest, ecParams.getCurve().getB());
        addFieldElement(digest, ecParams.getG().getAffineXCoord());
        addFieldElement(digest, ecParams.getG().getAffineYCoord());
        addFieldElement(digest, pubPoint.getAffineXCoord());
        addFieldElement(digest, pubPoint.getAffineYCoord());

        return digestDoFinal();
    }

    private void addUserID(Digest digest, byte[] userID) {
        int len = userID.length * 8;

        digest.update((byte) (len >>> 8));
        digest.update((byte) len);
        digest.update(userID, 0, userID.length);
    }

    private void addFieldElement(Digest digest, ECFieldElement v) {
        byte[] p = v.getEncoded();
        digest.update(p, 0, p.length);
    }

    private byte[] digestDoFinal() {
        byte[] result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);
        return result;
    }
}

