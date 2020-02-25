package org.tdf.crypto.keystore;

public class Cipherparams {
    public Cipherparams(String iv) {
        this.iv = iv;
    }

    public Cipherparams() {
    }

    public String iv;

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }
}
