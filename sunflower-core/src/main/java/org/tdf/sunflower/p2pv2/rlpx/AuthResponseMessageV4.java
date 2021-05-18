package org.tdf.sunflower.p2pv2.rlpx;

import org.spongycastle.math.ec.ECPoint;
import org.tdf.common.crypto.ECKey;
import org.tdf.common.util.ByteUtil;
import org.tdf.common.util.RLPUtil;
import org.tdf.rlpstream.Rlp;
import org.tdf.rlpstream.RlpList;

import static org.tdf.common.util.ByteUtil.toHexString;

public class AuthResponseMessageV4 {

    ECPoint ephemeralPublicKey; // 64 bytes - uncompressed and no type byte
    byte[] nonce; // 32 bytes
    int version = 4; // 4 bytes

    static AuthResponseMessageV4 decode(byte[] wire) {

        AuthResponseMessageV4 message = new AuthResponseMessageV4();

        RlpList params = RLPUtil.decodePartial(wire, 0);

        byte[] pubKeyBytes = params.bytesAt(0);

        byte[] bytes = new byte[65];
        System.arraycopy(pubKeyBytes, 0, bytes, 1, 64);
        bytes[0] = 0x04; // uncompressed
        message.ephemeralPublicKey = ECKey.CURVE.getCurve().decodePoint(bytes);

        message.nonce = params.bytesAt(1);

        byte[] versionBytes = params.bytesAt(2);
        message.version = ByteUtil.byteArrayToInt(versionBytes);

        return message;
    }

    public byte[] encode() {

        byte[] publicKey = new byte[64];
        System.arraycopy(ephemeralPublicKey.getEncoded(false), 1, publicKey, 0, publicKey.length);

        byte[] publicBytes = Rlp.encodeBytes(publicKey);
        byte[] nonceBytes = Rlp.encodeBytes(nonce);
        byte[] versionBytes = Rlp.encodeInt(version);

        return Rlp.encodeElements(publicBytes, nonceBytes, versionBytes);
    }

    @Override
    public String toString() {
        return "AuthResponseMessage{" +
            "\n  ephemeralPublicKey=" + ephemeralPublicKey +
            "\n  nonce=" + toHexString(nonce) +
            "\n  version=" + version +
            '}';
    }
}
