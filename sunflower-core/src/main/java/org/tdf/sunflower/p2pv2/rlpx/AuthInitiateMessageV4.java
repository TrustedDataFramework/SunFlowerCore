package org.tdf.sunflower.p2pv2.rlpx;

import org.spongycastle.math.ec.ECPoint;
import org.tdf.common.crypto.ECDSASignature;
import org.tdf.common.crypto.ECKey;
import org.tdf.common.util.ByteUtil;
import org.tdf.common.util.RLPUtil;
import org.tdf.rlpstream.Rlp;
import org.tdf.rlpstream.RlpList;

import static org.tdf.common.util.ByteUtil.toHexString;

import static org.spongycastle.util.BigIntegers.asUnsignedByteArray;
import static org.tdf.common.util.ByteUtil.merge;

public class AuthInitiateMessageV4 {

    ECDSASignature signature; // 65 bytes
    ECPoint publicKey; // 64 bytes - uncompressed and no type byte
    byte[] nonce; // 32 bytes
    int version = 4; // 4 bytes

    public AuthInitiateMessageV4() {
    }

    static AuthInitiateMessageV4 decode(byte[] wire) {
        AuthInitiateMessageV4 message = new AuthInitiateMessageV4();

        RlpList params = RLPUtil.decodePartial(wire, 0);

        byte[] signatureBytes = params.bytesAt(0);
        int offset = 0;
        byte[] r = new byte[32];
        byte[] s = new byte[32];
        System.arraycopy(signatureBytes, offset, r, 0, 32);
        offset += 32;
        System.arraycopy(signatureBytes, offset, s, 0, 32);
        offset += 32;
        int v = signatureBytes[offset] + 27;
        message.signature = ECDSASignature.fromComponents(r, s, (byte) v);

        byte[] publicKeyBytes = params.bytesAt(1);
        byte[] bytes = new byte[65];
        System.arraycopy(publicKeyBytes, 0, bytes, 1, 64);
        bytes[0] = 0x04; // uncompressed
        message.publicKey = ECKey.CURVE.getCurve().decodePoint(bytes);

        message.nonce = params.bytesAt(2);

        byte[] versionBytes = params.bytesAt(3);
        message.version = ByteUtil.byteArrayToInt(versionBytes);

        return message;
    }

    public byte[] encode() {

        byte[] rsigPad = new byte[32];
        byte[] rsig = asUnsignedByteArray(signature.r);
        System.arraycopy(rsig, 0, rsigPad, rsigPad.length - rsig.length, rsig.length);

        byte[] ssigPad = new byte[32];
        byte[] ssig = asUnsignedByteArray(signature.s);
        System.arraycopy(ssig, 0, ssigPad, ssigPad.length - ssig.length, ssig.length);

        byte[] publicKey = new byte[64];
        System.arraycopy(this.publicKey.getEncoded(false), 1, publicKey, 0, publicKey.length);

        byte[] sigBytes = Rlp.encodeBytes(merge(rsigPad, ssigPad, new byte[]{EncryptionHandshake.recIdFromSignatureV(signature.v)}));
        byte[] publicBytes = Rlp.encodeBytes(publicKey);
        byte[] nonceBytes = Rlp.encodeBytes(nonce);
        byte[] versionBytes = Rlp.encodeInt(version);

        return Rlp.encodeElements(sigBytes, publicBytes, nonceBytes, versionBytes);
    }

    @Override
    public String toString() {

        byte[] sigBytes = merge(asUnsignedByteArray(signature.r),
            asUnsignedByteArray(signature.s), new byte[]{EncryptionHandshake.recIdFromSignatureV(signature.v)});

        return "AuthInitiateMessage{" +
            "\n  sigBytes=" + toHexString(sigBytes) +
            "\n  publicKey=" + toHexString(publicKey.getEncoded(false)) +
            "\n  nonce=" + toHexString(nonce) +
            "\n  version=" + version +
            "\n}";
    }
}
