package org.tdf.crypto.ed25519;


import lombok.Value;
import org.tdf.crypto.KeyPair;

/**
 * Ed25519 keypair for signature and verifying
 */
@Value
public class Ed25519KeyPair implements KeyPair {
    private Ed25519PrivateKey privateKey;
    private Ed25519PublicKey publicKey;
}