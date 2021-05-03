package org.tdf.sunflower.state;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.tdf.common.crypto.ECKey;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.Transaction;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Address {
    static HexBytes EMPTY = HexBytes.fromBytes(new byte[Transaction.ADDRESS_LENGTH]);

    public static HexBytes empty() {
        return EMPTY;
    }


    public static HexBytes of(@NonNull String hex) {
        HexBytes ret = HexBytes.fromHex(hex);
        if (ret.size() == Transaction.ADDRESS_LENGTH) return ret;
        throw new RuntimeException("invalid hex, not an address");
    }

    public static HexBytes fromPrivate(HexBytes privateK){
        return fromPrivate(privateK.getBytes());
    }

    public static HexBytes fromPrivate(byte[] privateK){
        return HexBytes.fromBytes(ECKey.fromPrivate(privateK).getAddress());
    }

    public static HexBytes fromPub(HexBytes pub) {
        return HexBytes.fromBytes(ECKey.fromPublicOnly(pub.getBytes()).getAddress());
    }
}
