package org.tdf.sunflower.state;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.CryptoContext;

import static org.tdf.sunflower.state.Account.ADDRESS_SIZE;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Address {
    static HexBytes EMPTY = HexBytes.fromBytes(new byte[ADDRESS_SIZE]);

    public static HexBytes empty() {
        return EMPTY;
    }


    public static HexBytes of(@NonNull String hex) {
        HexBytes ret = HexBytes.fromHex(hex);
        if (ret.size() == ADDRESS_SIZE) return ret;
        if (ret.size() == CryptoContext.getPublicKeySize()) {
            return fromPublicKey(ret.getBytes());
        }

        throw new RuntimeException("invalid hex, not a public key or address");
    }

    public static HexBytes fromPublicKey(HexBytes publicKey) {
        return fromPublicKey(publicKey.getBytes());
    }

    public static HexBytes fromPublicKey(byte[] publicKey) {
        if (publicKey.length != CryptoContext.getPublicKeySize())
            throw new RuntimeException("invalid public key, length = " + publicKey.length);
        HexBytes ret = HexBytes.fromBytes(CryptoContext.hash(publicKey));
        return ret.slice(ret.size() - ADDRESS_SIZE, ret.size());
    }
}
