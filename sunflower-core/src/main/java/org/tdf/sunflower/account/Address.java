package org.tdf.sunflower.account;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.tdf.common.util.HexBytes;
import org.tdf.crypto.CryptoContext;
import org.tdf.crypto.KeyPair;
import org.tdf.crypto.ed25519.Ed25519;

import static org.tdf.sunflower.ApplicationConstants.ADDRESS_SIZE;
import static org.tdf.sunflower.ApplicationConstants.PUBLIC_KEY_SIZE;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Address {

    public static HexBytes of(@NonNull String hex) {
        HexBytes ret = HexBytes.fromHex(hex);
        if (ret.size() == ADDRESS_SIZE) return ret;
        if (ret.size() == PUBLIC_KEY_SIZE) {
            return fromPublicKey(ret.getBytes());
        }

        throw new RuntimeException("invalid hex, not a public key or address");
    }

    public static HexBytes fromPublicKey(byte[] publicKey) {
        if (publicKey.length != PUBLIC_KEY_SIZE)
            throw new RuntimeException("invalid public key, length = " + publicKey.length);
        HexBytes ret = HexBytes.fromBytes(CryptoContext.digest(publicKey));
        return ret.slice(ret.size() - ADDRESS_SIZE, ret.size());
    }

    public static void main(String[] args) {
        KeyPair keyPair =
                Ed25519.generateKeyPair();
        System.out.println("private key = " + HexBytes.fromBytes(keyPair.getPrivateKey().getEncoded()));
        System.out.println("public key = " + HexBytes.fromBytes(keyPair.getPublicKey().getEncoded()));
        System.out.println("address = " + fromPublicKey(keyPair.getPublicKey().getEncoded()));
    }
}
