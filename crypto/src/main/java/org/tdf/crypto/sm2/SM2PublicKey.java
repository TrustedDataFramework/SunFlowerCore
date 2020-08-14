package org.tdf.crypto.sm2;

import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.tdf.gmhelper.BCECUtil;
import org.tdf.gmhelper.SM2Util;
import org.tdf.crypto.PublicKey;

import java.io.IOException;

public class SM2PublicKey implements PublicKey {

    public SM2PublicKey(BCECPublicKey bcecPublicKey) {
        this.bcecPublicKey = bcecPublicKey;
    }

    public SM2PublicKey(byte[] pub) {
        this.bcecPublicKey = SM2Util.createBCECPublicKey(pub);
    }

    public BCECPublicKey getBcecPublicKey() {
        return bcecPublicKey;
    }

    private BCECPublicKey bcecPublicKey;

    public boolean verify(byte[] msg, byte[] signature) {//签名为r，s字节数组的原始拼接
        ECPublicKeyParameters pubKeyParameters = BCECUtil.convertPublicKeyToParameters(bcecPublicKey);
        byte[] derSignature = null;
        try {
            derSignature = SM2Util.encodeSM2SignToDER(signature);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return SM2Util.verify(pubKeyParameters, msg, derSignature);
    }

    public byte[] encrypt(byte[] msg) {
        return SM2Util.encrypt(getEncoded(), msg);
    }

    @Override
    public String getAlgorithm() {
        return SM2.getAlgorithm();
    }

    @Override
    public String getFormat() {
        return "";
    }

    @Override
    public byte[] getEncoded() {
        return bcecPublicKey.getQ().getEncoded(true);//目前公钥仅支持压缩格式
    }
}
