package org.tdf.gmhelper;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.asn1.*;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.*;
import org.bouncycastle.crypto.signers.SM2Signer;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.custom.gm.SM2P256V1Curve;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECFieldFp;
import java.security.spec.EllipticCurve;
import java.util.Arrays;

public class SM2Util extends GMBaseUtil {
    //////////////////////////////////////////////////////////////////////////////////////
    /*
     * 以下为SM2推荐曲线参数
     */
    public static final SM2P256V1Curve CURVE = new SM2P256V1Curve();
    public final static BigInteger SM2_ECC_P = CURVE.getQ();
    public final static BigInteger SM2_ECC_A = CURVE.getA().toBigInteger();
    public final static BigInteger SM2_ECC_B = CURVE.getB().toBigInteger();
    public final static BigInteger SM2_ECC_N = CURVE.getOrder();
    public final static BigInteger SM2_ECC_H = CURVE.getCofactor();
    public final static BigInteger SM2_ECC_GX = new BigInteger(
            "32C4AE2C1F1981195F9904466A39C9948FE30BBFF2660BE1715A4589334C74C7", 16);
    public final static BigInteger SM2_ECC_GY = new BigInteger(
            "BC3736A2F4F6779C59BDCEE36B692153D0A9877CC62A474002DF32E52139F0A0", 16);
    public static final ECPoint G_POINT = CURVE.createPoint(SM2_ECC_GX, SM2_ECC_GY);
    public static final ECDomainParameters DOMAIN_PARAMS = new ECDomainParameters(CURVE, G_POINT,
            SM2_ECC_N, SM2_ECC_H);
    public static final int CURVE_LEN = BCECUtil.getCurveLength(DOMAIN_PARAMS);
    //////////////////////////////////////////////////////////////////////////////////////

    public static final EllipticCurve JDK_CURVE = new EllipticCurve(new ECFieldFp(SM2_ECC_P), SM2_ECC_A, SM2_ECC_B);
    public static final java.security.spec.ECPoint JDK_G_POINT = new java.security.spec.ECPoint(
            G_POINT.getAffineXCoord().toBigInteger(), G_POINT.getAffineYCoord().toBigInteger());
    public static final java.security.spec.ECParameterSpec JDK_EC_SPEC = new java.security.spec.ECParameterSpec(
            JDK_CURVE, JDK_G_POINT, SM2_ECC_N, SM2_ECC_H.intValue());

    //////////////////////////////////////////////////////////////////////////////////////

    public static final int SM3_DIGEST_LENGTH = 32;
    public static final byte[] WITH_ID = "userid@soie-chain.com".getBytes(StandardCharsets.US_ASCII);
    public static final byte[] DEFAULT_USER_ID = {0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38};
    public static final ECKeyPairGenerator KEY_PAIR_GENERATOR = new ECKeyPairGenerator();
    public static final ECKeyGenerationParameters EC_KEY_GENERATION_PARAMETERS =
            new ECKeyGenerationParameters(new ECDomainParameters(CURVE, G_POINT, SM2_ECC_N), new SecureRandom());

    static {
        KEY_PAIR_GENERATOR.init(EC_KEY_GENERATION_PARAMETERS);
    }

    /**
     * 生成ECC密钥对
     *
     * @return ECC密钥对
     */
    public static AsymmetricCipherKeyPair generateKeyPairParameter() {
        SecureRandom random = new SecureRandom();
        return BCECUtil.generateKeyPairParameter(DOMAIN_PARAMS, random);
    }

    public static KeyPair generateKeyPair() throws NoSuchProviderException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException {
        SecureRandom random = new SecureRandom();
        return BCECUtil.generateKeyPair(DOMAIN_PARAMS, random);
    }

    /**
     * 通过字节数组格式的私钥创建私钥对象
     *
     * @param priv private key
     * @return private key object
     */
    public static BCECPrivateKey createBCECPrivateKey(byte[] priv) {
        BigInteger privkey = new BigInteger(1, priv);
        ECPrivateKeyParameters ecPrivateKeyParameters = BCECUtil.createECPrivateKeyParameters(privkey, DOMAIN_PARAMS);
        BCECPublicKey bcecPublicKey = SM2Util.getBCECPublicKeyFromPrivateKey(priv);
        return new BCECPrivateKey("sm2", ecPrivateKeyParameters, bcecPublicKey, (java.security.spec.ECParameterSpec) null, BouncyCastleProvider.CONFIGURATION);
    }

    /**
     * 通过字节数组格式的私钥创建公钥对象
     *
     * @param priv
     * @return
     */
    public static BCECPublicKey getBCECPublicKeyFromPrivateKey(byte[] priv) {
        BigInteger d = new BigInteger(1, priv);
        ECPoint ecPoint = G_POINT.multiply(d);
        return SM2Util.createBCECPublicKey(ecPoint.getEncoded(true));
    }

    /**
     * 通过字节数组格式的公钥创建公钥对象
     *
     * @param pub
     * @return
     */
    public static BCECPublicKey createBCECPublicKey(byte[] pub) {
        ECPublicKeyParameters ecPublicKeyParameters = BCECUtil.createECPublicKeyParameters(pub, CURVE, DOMAIN_PARAMS);
        BCECPublicKey bcecPublicKey = new BCECPublicKey("sm2", ecPublicKeyParameters, JDK_EC_SPEC, BouncyCastleProvider.CONFIGURATION);
        return bcecPublicKey;
    }

    /**
     * 只获取私钥里的d，32字节
     *
     * @param privateKey
     * @return
     */
    public static byte[] getRawPrivateKey(BCECPrivateKey privateKey) {
        return fixToCurveLengthBytes(privateKey.getD().toByteArray());
    }

    /**
     * 只获取公钥里的XY分量，64字节
     *
     * @param publicKey
     * @return
     */
    public static byte[] getRawPublicKey(BCECPublicKey publicKey) {
        byte[] src65 = publicKey.getQ().getEncoded(false);
        byte[] rawXY = new byte[CURVE_LEN * 2];//SM2的话这里应该是64字节
        System.arraycopy(src65, 1, rawXY, 0, rawXY.length);
        return rawXY;
    }

    public static byte[] encrypt(BCECPublicKey pubKey, byte[] srcData) {
        ECPublicKeyParameters pubKeyParameters = BCECUtil.convertPublicKeyToParameters(pubKey);
        return encrypt(pubKeyParameters, srcData);
    }

    /**
     * ECC公钥加密
     *
     * @param pubKeyParameters ECC公钥
     * @param srcData          源数据
     * @return SM2密文，实际包含三部分：ECC公钥、真正的密文、公钥和原文的SM3-HASH值
     */
    public static byte[] encrypt(ECPublicKeyParameters pubKeyParameters, byte[] srcData) {
        SM2Engine engine = new SM2Engine();
        ParametersWithRandom pwr = new ParametersWithRandom(pubKeyParameters, new SecureRandom());
        engine.init(true, pwr);
        return engine.encrypt(srcData, srcData.length);
    }

    public static byte[] decrypt(BCECPrivateKey priKey, byte[] sm2Cipher) {
        ECPrivateKeyParameters priKeyParameters = BCECUtil.convertPrivateKeyToParameters(priKey);
        return decrypt(priKeyParameters, sm2Cipher);
    }

    /**
     * ECC私钥解密
     *
     * @param priKeyParameters ECC私钥
     * @param sm2Cipher        SM2密文，实际包含三部分：ECC公钥、真正的密文、公钥和原文的SM3-HASH值
     * @return 原文
     */
    public static byte[] decrypt(ECPrivateKeyParameters priKeyParameters, byte[] sm2Cipher) {
        SM2Engine engine = new SM2Engine();
        engine.init(false, priKeyParameters);
        return engine.decrypt(sm2Cipher, sm2Cipher.length);
    }

    /**
     * 分解SM2密文
     *
     * @param cipherText SM2密文
     * @return
     */
    public static SM2Cipher parseSM2Cipher(byte[] cipherText) {
        int curveLength = BCECUtil.getCurveLength(DOMAIN_PARAMS);
        return parseSM2Cipher(curveLength, SM3_DIGEST_LENGTH, cipherText);
    }

    /**
     * 分解SM2密文
     *
     * @param curveLength  ECC曲线长度
     * @param digestLength HASH长度
     * @param cipherText   SM2密文
     * @return
     */
    public static SM2Cipher parseSM2Cipher(int curveLength, int digestLength,
                                           byte[] cipherText) {
        byte[] c1 = new byte[curveLength * 2 + 1];
        System.arraycopy(cipherText, 0, c1, 0, c1.length);
        byte[] c3 = new byte[digestLength];
        System.arraycopy(cipherText, c1.length, c3, 0, c3.length);
        byte[] c2 = new byte[cipherText.length - c1.length - digestLength];
        System.arraycopy(cipherText, c1.length + c3.length, c2, 0, c2.length);
        SM2Cipher result = new SM2Cipher();
        result.setC1(c1);
        result.setC2(c2);
        result.setC3(c3);
        result.setCipherText(cipherText);
        return result;
    }

    /**
     * DER编码C1C2C3密文（根据《SM2密码算法使用规范》 GM/T 0009-2012）
     *
     * @param cipher
     * @return
     * @throws IOException
     */
    public static byte[] encodeSM2CipherToDER(byte[] cipher) throws IOException {
        int curveLength = BCECUtil.getCurveLength(DOMAIN_PARAMS);
        return encodeSM2CipherToDER(curveLength, SM3_DIGEST_LENGTH, cipher);
    }

    /**
     * DER编码C1C2C3密文（根据《SM2密码算法使用规范》 GM/T 0009-2012）
     *
     * @param curveLength
     * @param digestLength
     * @param cipher
     * @return
     * @throws IOException
     */
    public static byte[] encodeSM2CipherToDER(int curveLength, int digestLength, byte[] cipher)
            throws IOException {
        int startPos = 1;

        byte[] c1x = new byte[curveLength];
        System.arraycopy(cipher, startPos, c1x, 0, c1x.length);
        startPos += c1x.length;

        byte[] c1y = new byte[curveLength];
        System.arraycopy(cipher, startPos, c1y, 0, c1y.length);
        startPos += c1y.length;

        byte[] c3 = new byte[digestLength];
        System.arraycopy(cipher, startPos, c3, 0, c3.length);
        startPos += c3.length;
        byte[] c2 = new byte[cipher.length - c1x.length - c1y.length - 1 - digestLength];
        System.arraycopy(cipher, startPos, c2, 0, c2.length);

        ASN1Encodable[] arr = new ASN1Encodable[4];
        arr[0] = new ASN1Integer(c1x);
        arr[1] = new ASN1Integer(c1y);
        arr[2] = new DEROctetString(c3);
        arr[3] = new DEROctetString(c2);
        DERSequence ds = new DERSequence(arr);
        return ds.getEncoded(ASN1Encoding.DER);
    }

    /**
     * 解DER编码密文（根据《SM2密码算法使用规范》 GM/T 0009-2012）
     *
     * @param derCipher
     * @return
     */
    public static byte[] decodeDERSM2Cipher(byte[] derCipher) {
        ASN1Sequence as = DERSequence.getInstance(derCipher);
        byte[] c1x = ((ASN1Integer) as.getObjectAt(0)).getValue().toByteArray();
        byte[] c1y = ((ASN1Integer) as.getObjectAt(1)).getValue().toByteArray();
        byte[] c3 = ((DEROctetString) as.getObjectAt(2)).getOctets();
        byte[] c2 = ((DEROctetString) as.getObjectAt(3)).getOctets();
        c1x = fixToCurveLengthBytes(c1x);
        c1y = fixToCurveLengthBytes(c1y);
        int pos = 0;
        byte[] cipherText = new byte[1 + c1x.length + c1y.length + c2.length + c3.length];

        final byte uncompressedFlag = 0x04;
        cipherText[0] = uncompressedFlag;
        pos += 1;

        System.arraycopy(c1x, 0, cipherText, pos, c1x.length);
        pos += c1x.length;

        System.arraycopy(c1y, 0, cipherText, pos, c1y.length);
        pos += c1y.length;

        System.arraycopy(c3, 0, cipherText, pos, c3.length);
        pos += c3.length;

        System.arraycopy(c2, 0, cipherText, pos, c2.length);

        return cipherText;
    }

    public static byte[] sign(BCECPrivateKey priKey, byte[] srcData) throws CryptoException {
        ECPrivateKeyParameters priKeyParameters = BCECUtil.convertPrivateKeyToParameters(priKey);
        return sign(priKeyParameters, WITH_ID, srcData);
    }

    /**
     * ECC私钥签名
     * 不指定withId，则默认withId为字节数组:"1234567812345678".getBytes()
     *
     * @param priKeyParameters ECC私钥
     * @param srcData          源数据
     * @return 签名
     * @throws CryptoException
     */
    public static byte[] sign(ECPrivateKeyParameters priKeyParameters, byte[] srcData) throws CryptoException {
        return sign(priKeyParameters, null, srcData);
    }

    public static byte[] sign(BCECPrivateKey priKey, byte[] withId, byte[] srcData) throws CryptoException {
        ECPrivateKeyParameters priKeyParameters = BCECUtil.convertPrivateKeyToParameters(priKey);
        return sign(priKeyParameters, withId, srcData);
    }

    /**
     * ECC私钥签名
     *
     * @param priKeyParameters ECC私钥
     * @param withId           可以为null，若为null，则默认withId为字节数组:"1234567812345678".getBytes()
     * @param srcData          源数据
     * @return 签名
     * @throws CryptoException
     */
    public static byte[] sign(ECPrivateKeyParameters priKeyParameters, byte[] withId, byte[] srcData)
            throws CryptoException {
        SM2Signer signer = new SM2Signer();
        CipherParameters param = null;
        ParametersWithRandom pwr = new ParametersWithRandom(priKeyParameters, new SecureRandom());
        if (withId != null) {
            param = new ParametersWithID(pwr, withId);
        } else {
            param = pwr;
        }
        signer.init(true, param);
        signer.update(srcData, 0, srcData.length);
        return signer.generateSignature();
    }

    /**
     * 将DER编码的SM2签名解析成64字节的纯R+S字节流
     *
     * @param derSign
     * @return
     */
    public static byte[] decodeDERSM2Sign(byte[] derSign) {
        ASN1Sequence as = DERSequence.getInstance(derSign);
        byte[] rBytes = ((ASN1Integer) as.getObjectAt(0)).getValue().toByteArray();
        byte[] sBytes = ((ASN1Integer) as.getObjectAt(1)).getValue().toByteArray();
        //由于大数的补0规则，所以可能会出现33个字节的情况，要修正回32个字节
        rBytes = fixToCurveLengthBytes(rBytes);
        sBytes = fixToCurveLengthBytes(sBytes);
        byte[] rawSign = new byte[rBytes.length + sBytes.length];
        System.arraycopy(rBytes, 0, rawSign, 0, rBytes.length);
        System.arraycopy(sBytes, 0, rawSign, rBytes.length, sBytes.length);
        return rawSign;
    }

    /**
     * 把64字节的纯R+S字节流转换成DER编码字节流
     *
     * @param rawSign
     * @return
     * @throws IOException
     */
    public static byte[] encodeSM2SignToDER(byte[] rawSign) throws IOException {
        //要保证大数是正数
        BigInteger r = new BigInteger(1, extractBytes(rawSign, 0, 32));
        BigInteger s = new BigInteger(1, extractBytes(rawSign, 32, 32));
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(new ASN1Integer(r));
        v.add(new ASN1Integer(s));
        return new DERSequence(v).getEncoded(ASN1Encoding.DER);
    }

    public static boolean verify(BCECPublicKey pubKey, byte[] srcData, byte[] sign) {
        ECPublicKeyParameters pubKeyParameters = BCECUtil.convertPublicKeyToParameters(pubKey);
        return verify(pubKeyParameters, null, srcData, sign);
    }

    /**
     * ECC公钥验签
     * 不指定withId，则默认withId为字节数组:"1234567812345678".getBytes()
     *
     * @param pubKeyParameters ECC公钥
     * @param srcData          源数据
     * @param sign             签名
     * @return 验签成功返回true，失败返回false
     */
    public static boolean verify(ECPublicKeyParameters pubKeyParameters, byte[] srcData, byte[] sign) {
        return verify(pubKeyParameters, SM2Util.WITH_ID, srcData, sign);
    }

    public static boolean verify(BCECPublicKey pubKey, byte[] withId, byte[] srcData, byte[] sign) {
        ECPublicKeyParameters pubKeyParameters = BCECUtil.convertPublicKeyToParameters(pubKey);
        return verify(pubKeyParameters, withId, srcData, sign);
    }

    /**
     * ECC公钥验签
     *
     * @param pubKeyParameters ECC公钥
     * @param withId           可以为null，若为null，则默认withId为字节数组:"1234567812345678".getBytes()
     * @param srcData          源数据
     * @param sign             签名
     * @return 验签成功返回true，失败返回false
     */
    public static boolean verify(ECPublicKeyParameters pubKeyParameters, byte[] withId, byte[] srcData, byte[] sign) {
        SM2Signer signer = new SM2Signer();
        CipherParameters param;
        if (withId != null) {
            param = new ParametersWithID(pubKeyParameters, withId);
        } else {
            param = pubKeyParameters;
        }
        signer.init(false, param);
        signer.update(srcData, 0, srcData.length);
        return signer.verifySignature(sign);
    }

    private static byte[] extractBytes(byte[] src, int offset, int length) {
        byte[] result = new byte[length];
        System.arraycopy(src, offset, result, 0, result.length);
        return result;
    }

    private static byte[] fixToCurveLengthBytes(byte[] src) {
        if (src.length == CURVE_LEN) {
            return src;
        }

        byte[] result = new byte[CURVE_LEN];
        if (src.length > CURVE_LEN) {
            System.arraycopy(src, src.length - result.length, result, 0, result.length);
        } else {
            System.arraycopy(src, 0, result, result.length - src.length, src.length);
        }
        return result;
    }


    /**
     * Returns the values from each provided array combined into a single array. For example, {@code
     * concat(new byte[] {a, b}, new byte[] {}, new byte[] {c}} returns the array {@code {a, b, c}}.
     *
     * @param arrays zero or more {@code byte} arrays
     * @return a single array containing all the values from the source arrays, in order
     */
    public static byte[] concat(byte[]... arrays) {
        int length = 0;
        for (byte[] array : arrays) {
            length += array.length;
        }
        byte[] result = new byte[length];
        int pos = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, pos, array.length);
            pos += array.length;
        }
        return result;
    }

    /**
     * 加密（采用新版本的分组方式C1 | C2 | C3）
     *
     * @param denData 待加密字符串
     * @return
     */
    public static byte[] encrypt(@NonNull byte[] publicKey, @NonNull byte[] msg) {
        if (msg.length == 0)
            throw new RuntimeException("sm2 encrypt failed: empty msg");
        byte[] source = new byte[msg.length];
        System.arraycopy(msg, 0, source, 0, msg.length);


        ECPoint userKey = CURVE.decodePoint(publicKey);
        Cipher cipher = new Cipher();
        ECPoint c1 = cipher.initEncrypt(userKey);
        cipher.encrypt(source);
        byte[] c3 = new byte[32];
        cipher.doFinal(c3);
        byte[] c1Encoded = c1.getEncoded(false);
        if (c1Encoded.length > 64)
            c1Encoded = Arrays.copyOfRange(c1Encoded, c1Encoded.length - 64, c1Encoded.length);
        // C1 | C2 | C3
        return concat(
                c1Encoded,
                source,
                c3);
    }

    @RequiredArgsConstructor
    static class BytesReader {
        private final byte[] bytes;
        private int current;

        public byte[] readN(int n) {
            byte[] ret = Arrays.copyOfRange(bytes, current, current + n);
            current += n;
            return ret;
        }

        public int available() {
            return bytes.length - current;
        }

        public byte read() {
            return bytes[current++];
        }

        public byte peek() {
            return bytes[current];
        }

        public BytesReader skip(int n) {
            this.current += n;
            return this;
        }
    }

    /**
     * 解密（采用新版本的分组方式C1 | C2 | C3）
     *
     * @param encryptedData 待解密字符串
     * @return
     */
    public static byte[] decrypt(@NonNull byte[] sk, @NonNull byte[] encrypted) {


        /** 分组方式
         *  分解加密字串 C1 | C2 | C3
         * （C1 = C1标志位2位 + C1实体部分128位 = 130）
         * （C3 = C3实体部分64位  = 64）
         * （C2 = encryptedData.length * 2 - C1长度  - C2长度）*/
        BytesReader r = new BytesReader(encrypted);
        byte[] c1Bytes = r.peek() == 0x04 ?
                r.skip(1).readN(64) :
                r.readN(64);

        byte[] c1x = Arrays.copyOfRange(c1Bytes, 0, 32);
        byte[] c1y = Arrays.copyOfRange(c1Bytes, 32, c1Bytes.length);

        int c2Len = r.available() - 32;
        byte[] c2 = r.readN(c2Len);
        byte[] c3 = r.readN(32);


        BigInteger userD = new BigInteger(1, sk);

        //通过C1实体字节来生成ECPoint
        ECPoint c1 = CURVE.createPoint(new BigInteger(c1x), new BigInteger(c1y));

        Cipher cipher = new Cipher();
        cipher.initDecrypt(userD, c1);
        cipher.decrypt(c2);
        cipher.doFinal(c3);
        return c2;
    }

    public static class Cipher {

        private int ct;
        private ECPoint p2;
        private SM3Digest sm3KeyBase;
        private SM3Digest sm3c3;
        private byte key[];
        private byte keyOff;

        public Cipher() {
            this.ct = 1;
            this.key = new byte[32];
            this.keyOff = 0;
        }

        private void reset() {
            this.sm3KeyBase = new SM3Digest();
            this.sm3c3 = new SM3Digest();

            byte p[] = p2.getXCoord().getEncoded();
            this.sm3KeyBase.update(p, 0, p.length);
            this.sm3c3.update(p, 0, p.length);

            p = p2.getYCoord().getEncoded();
            this.sm3KeyBase.update(p, 0, p.length);
            this.ct = 1;
            nextKey();
        }

        private void nextKey() {
            SM3Digest sm3KeyCur = new SM3Digest(this.sm3KeyBase);
            sm3KeyCur.update((byte) (ct >> 24 & 0xff));
            sm3KeyCur.update((byte) (ct >> 16 & 0xff));
            sm3KeyCur.update((byte) (ct >> 8 & 0xff));
            sm3KeyCur.update((byte) (ct & 0xff));
            sm3KeyCur.doFinal(key, 0);
            this.keyOff = 0;
            this.ct++;
        }

        public void encrypt(byte[] data) {
            this.sm3c3.update(data, 0, data.length);
            for (int i = 0; i < data.length; i++) {
                if (keyOff == key.length) {
                    nextKey();
                }
                data[i] ^= key[keyOff++];
            }
        }

        public ECPoint initEncrypt(ECPoint userKey) {
            AsymmetricCipherKeyPair key = KEY_PAIR_GENERATOR.generateKeyPair();
            ECPrivateKeyParameters ecpriv = (ECPrivateKeyParameters) key.getPrivate();
            ECPublicKeyParameters ecpub = (ECPublicKeyParameters) key.getPublic();
            ECPoint c1 = ecpub.getQ();

            BigInteger k = ecpriv.getD();
            this.p2 = userKey.multiply(k).normalize();
            reset();
            return c1.normalize();
        }

        public void initDecrypt(BigInteger userD, ECPoint c1) {
            this.p2 = c1.multiply(userD).normalize();
            reset();
        }

        public void decrypt(byte[] data) {
            for (int i = 0; i < data.length; i++) {
                if (keyOff == key.length) {
                    nextKey();
                }
                data[i] ^= key[keyOff++];
            }

            this.sm3c3.update(data, 0, data.length);
        }

        public void doFinal(byte[] c3) {
            byte[] p = p2.getYCoord().getEncoded();
            this.sm3c3.update(p, 0, p.length);
            this.sm3c3.doFinal(c3, 0);
            reset();
        }
    }

}
