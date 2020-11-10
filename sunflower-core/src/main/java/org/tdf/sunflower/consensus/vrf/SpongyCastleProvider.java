package org.tdf.sunflower.consensus.vrf;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.security.Provider;
import java.security.Security;

public final class SpongyCastleProvider {

    public static Provider getInstance() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        private static final Provider INSTANCE;

        static {
            Provider p = Security.getProvider("SC");

            INSTANCE = (p != null) ? p : new BouncyCastleProvider();

            INSTANCE.put("MessageDigest.ETH-KECCAK-256", "org.tdf.sunflower.consensus.vrf.crypto.hash.Keccak256");
            INSTANCE.put("MessageDigest.ETH-KECCAK-512", "org.tdf.sunflower.consensus.vrf.crypto.hash.Keccak512");
        }
    }
}
