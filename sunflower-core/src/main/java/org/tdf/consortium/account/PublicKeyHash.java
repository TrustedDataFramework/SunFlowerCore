package org.tdf.consortium.account;

import org.apache.commons.codec.binary.Hex;

import java.util.Arrays;
import java.util.Optional;

import static org.tdf.consortium.account.Utils.*;
import static org.tdf.consortium.ApplicationConstants.PUBLIC_KEY_SIZE;

public class PublicKeyHash {
    private byte[] publicKeyHash;
    private String address;
    private String hex;
    public static PublicKeyHash fromPublicKey(byte[] publicKey){
        return new PublicKeyHash(publicKeyToHash(publicKey));
    }

    /**
     * create a publicKey hash from public key hex or address
     * @param hex
     * @return publicKeyHash
     */
    public static Optional<PublicKeyHash> from(String hex){
        byte[] publicKeyHash;
        try {
            publicKeyHash = Hex.decodeHex(hex);
            if (publicKeyHash.length == PUBLIC_KEY_SIZE) {
                return Optional.of(fromPublicKey(publicKeyHash));
            }
            if(publicKeyHash.length == PUBLIC_KEY_HASH_LENGTH){
                return Optional.of(new PublicKeyHash(publicKeyHash));
            }
        } catch (Exception e) {
            return addressToPublicKeyHash(hex).map(PublicKeyHash::new);
        }
        return Optional.empty();
    }

    public PublicKeyHash(byte[] publicKeyHash) {
        this.publicKeyHash = publicKeyHash;
    }

    public String getAddress(){
        if (address == null) address = publicKeyHashToAddress(publicKeyHash);
        return address;
    }

    public byte[] getPublicKeyHash() {
        return publicKeyHash;
    }

    public String getHex(){
        if (hex == null) hex = Hex.encodeHexString(publicKeyHash);
        return hex;
    }

    public String toString(){
        return getHex();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PublicKeyHash that = (PublicKeyHash) o;
        return Arrays.equals(publicKeyHash, that.publicKeyHash);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(publicKeyHash);
    }
}
