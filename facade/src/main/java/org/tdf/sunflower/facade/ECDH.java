package org.tdf.sunflower.facade;

public interface ECDH {
    byte[] exchange(boolean initiator, byte[] sk, byte[] pk);

    default byte[] exchange(byte[] sk, byte[] pk) {
        return exchange(false, sk, pk);
    }
}
