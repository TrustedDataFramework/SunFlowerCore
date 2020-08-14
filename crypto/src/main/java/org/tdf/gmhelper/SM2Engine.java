package org.tdf.gmhelper;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.math.ec.ECConstants;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECMultiplier;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.FixedPointCombMultiplier;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import org.bouncycastle.util.BigIntegers;

/**
 * SM2 public key encryption engine - based on https://tools.ietf.org/html/draft-shen-sm2-ecdsa-02
 */
public class SM2Engine
{
    private final Digest digest;

    private boolean forEncryption;
    private ECKeyParameters ecKey;
    private ECDomainParameters ecParams;
    private int curveLength;
    private SecureRandom random;

    public SM2Engine()
    {
        this(new SM3Digest());
    }

    public SM2Engine(Digest digest)
    {
        this.digest = digest;
    }

    public void init(boolean forEncryption, CipherParameters param)
    {
        this.forEncryption = forEncryption;

        if (forEncryption)
        {
            ParametersWithRandom rParam = (ParametersWithRandom)param;

            ecKey = (ECKeyParameters)rParam.getParameters();
            ecParams = ecKey.getParameters();

            random = rParam.getRandom();
        }
        else
        {
            ecKey = (ECKeyParameters)param;
            ecParams = ecKey.getParameters();
        }

        curveLength = (ecParams.getCurve().getFieldSize() + 7) / 8;
    }

    protected ECMultiplier createBasePointMultiplier()
    {
        return new FixedPointCombMultiplier();
    }
    private boolean allZero(byte[] buffer) {
        for (int i = 0; i < buffer.length; i++) {
            if (buffer[i] != 0)
                return false;
        }
        return true;
    }
    public byte[] encrypt(byte[] in, int inLen)
    {
        byte[] C1Buffer;
        ECPoint kpb;
        byte[] t;
        do {
            // A1
            BigInteger k = nextK();

            // A2
            ECPoint C1 = createBasePointMultiplier().multiply(ecParams.getG(), k).normalize();
            C1Buffer = C1.getEncoded(false);
            if(C1Buffer.length > 64){
                C1Buffer = Arrays.copyOfRange(C1Buffer, C1Buffer.length - 64, C1Buffer.length);
            }
            // A3
            BigInteger h = ecParams.getH();
            if (h != null) {
                ECPoint S = ((ECPublicKeyParameters)ecKey).getQ().multiply(h).normalize();
                if (S.isInfinity())
                    throw new IllegalStateException();
            }

            // A4
            kpb = ((ECPublicKeyParameters)ecKey).getQ().multiply(k).normalize();

            // A5

            byte[] kpbBytes = byteMerger(kpb.getAffineXCoord().getEncoded(), kpb.getAffineYCoord().getEncoded() );
            t = KDF(digest, kpbBytes, in.length);

        } while (allZero(t));

        // A6
        byte[] C2 = new byte[in.length];
        for (int i = 0; i < in.length; i++) {
            C2[i] = (byte) (in[i] ^ t[i]);
        }

        // A7
        byte[] C3 = new byte[digest.getDigestSize()];
        addFieldElement(digest, kpb.getAffineXCoord());
        digest.update(in, 0, in.length);
        addFieldElement(digest, kpb.getAffineYCoord());
        digest.doFinal(C3, 0);

        // A8

        byte[] encryptResult = new byte[C1Buffer.length + C2.length + C3.length];

        System.arraycopy(C1Buffer, 0, encryptResult, 0, C1Buffer.length);
        System.arraycopy(C2, 0, encryptResult, C1Buffer.length + C3.length, C2.length);
        System.arraycopy(C3, 0, encryptResult, C1Buffer.length, C3.length);
        return encryptResult;
    }

    public byte[] decrypt(byte[] in, int inLen)
    {
        byte[] C1Byte = new byte[curveLength * 2 + 1];
        System.arraycopy(in, 0, C1Byte, 0, C1Byte.length);
        // B1
        ECPoint C1 = ecParams.getCurve().decodePoint(C1Byte).normalize();

        // B2
        BigInteger h = ecParams.getH();
        if (h != null) {
            ECPoint S = C1.multiply(h);
            if (S.isInfinity())
                throw new IllegalStateException();
        }
        // B3
        ECPoint dBC1 = C1.multiply(((ECPrivateKeyParameters)ecKey).getD()).normalize();

        // B4
        //byte[] dBC1Bytes = dBC1.getEncoded(false);
        int klen = in.length - (curveLength * 2 + 1) - digest.getDigestSize();
        byte[] dBC1Bytes = byteMerger(dBC1.getAffineXCoord().getEncoded(), dBC1.getAffineYCoord().getEncoded());
        byte[] t = KDF(digest, dBC1Bytes, klen);

        if (allZero(t)) {
            System.err.println("all zero");
            throw new IllegalStateException();
        }

        // B5
        byte[] M = new byte[klen];
        for (int i = 0; i < M.length; i++) {
            M[i] = (byte) (in[C1Byte.length + digest.getDigestSize() + i] ^ t[i]);
        }


        // B6
        byte[] C3 = new byte[digest.getDigestSize()];


        System.arraycopy(in, C1Byte.length, C3, 0, digest.getDigestSize());
        addFieldElement(digest, dBC1.getAffineXCoord());
        digest.update(M, 0, M.length);
        addFieldElement(digest, dBC1.getAffineYCoord());
        byte[] u = new byte[digest.getDigestSize()];
        digest.doFinal(u, 0);
        // B7
        if (Arrays.equals(u, C3)) {
            return M;
        } else {
            return null;
        }
    }

    public static byte[] toByteArray(int i) {
        byte[] byteArray = new byte[4];
        byteArray[0] = (byte) (i >>> 24);
        byteArray[1] = (byte) ((i & 0xFFFFFF) >>> 16);
        byteArray[2] = (byte) ((i & 0xFFFF) >>> 8);
        byteArray[3] = (byte) (i & 0xFF);
        return byteArray;
    }
    public static byte[] byteMerger(byte[] bt1, byte[] bt2){
        byte[] bt3 = new byte[bt1.length+bt2.length];
        System.arraycopy(bt1, 0, bt3, 0, bt1.length);
        System.arraycopy(bt2, 0, bt3, bt1.length, bt2.length);
        return bt3;
    }
    /**
     * 密钥派生函数
     *
     * @param digest 摘要函数
     * @param Z input
     * @param klen 生成klen字节数长度的密钥
     *
     */
    public static byte[] KDF(Digest digest, byte[] Z, int klen) {
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
            return last;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private BigInteger nextK()
    {
        int qBitLength = ecParams.getN().bitLength();

        BigInteger k;
        do
        {
            k = BigIntegers.createRandomBigInteger(qBitLength, random);
        }
        while (k.equals(ECConstants.ZERO) || k.compareTo(ecParams.getN()) >= 0);

        return k;
    }

    private void addFieldElement(Digest digest, ECFieldElement v)
    {
        byte[] p = BigIntegers.asUnsignedByteArray(curveLength, v.toBigInteger());
        digest.update(p, 0, p.length);
    }
}
