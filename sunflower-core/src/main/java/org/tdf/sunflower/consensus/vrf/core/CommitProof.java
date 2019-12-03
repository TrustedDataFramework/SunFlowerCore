package org.tdf.sunflower.consensus.vrf.core;


import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tdf.sunflower.consensus.vrf.HashUtil;
import org.tdf.sunflower.util.RLPList;
import org.tdf.sunflower.util.RLPUtils;
import org.tdf.crypto.CryptoException;
import org.tdf.crypto.PrivateKey;
import org.tdf.crypto.PublicKey;
import org.tdf.crypto.ed25519.Ed25519PublicKey;

import static org.tdf.sunflower.util.ByteUtil.isNullOrZeroArray;
import static org.tdf.sunflower.util.ByteUtil.toHexString;

/**
 * @author James Hu
 * @since 2019/5/17
 */
public class CommitProof {

    private static final Logger logger = LoggerFactory.getLogger("CommitProve");

    /* Information of committer owner */
    /* VRF prove */
    private VrfProof vrfProof;
    /* The 160-bit address to which all fees collected from the
     * successful mining of this block be transferred; formally */
    private byte[] coinbase;

    /* Identifier of proposal block */
    private BlockIdentifier blockIdentifier;

    /* Signature of commit proof body: {vrfProof, coinbase, blockIdentifier} */
    private byte[] signature;

    private byte[] rlpEncoded;
    private boolean parsed = false;

    private byte[] hashCache;

    public CommitProof(VrfProof vrfProof, byte[] coinbase, BlockIdentifier blockIdentifier, PrivateKey sk) {

        this.vrfProof = vrfProof;

        this.coinbase = coinbase;

        this.blockIdentifier = blockIdentifier;

        if (sign(sk) == null) {
            logger.error("Fail to sign in constructor");
        }

        this.parsed = true;
    }

    public boolean verify() {
        byte[] vrfPk = this.vrfProof.getVrfPk();
        if (isNullOrZeroArray(vrfPk) || isNullOrZeroArray(signature)) {
            logger.error("Empty signature to verify, CommitProof {}", this.toString());
            return false;
        }

        byte[] seed = getSignatureSeed();

        PublicKey verifier = new Ed25519PublicKey(vrfPk);

        boolean verified = verifier.verify(seed, signature);
        if (!verified) {
            logger.error("Wrong signature to verify, CommitProof {}", this.toString());
        }

        return verified;
    }

    public CommitProof(byte[] rlpRawData) {
        logger.debug("new from [" + toHexString(rlpRawData) + "]");

        this.rlpEncoded = rlpRawData;
    }

    private synchronized void parseRLP() {
        if (parsed) return;

        RLPList params = RLPUtils.decode2(rlpEncoded);
        RLPList rlpProof = (RLPList) params.get(0);

        // Parse body of commit proof
        RLPList rlpBody = (RLPList) rlpProof.get(0);
        decodeBody(rlpBody);

        // Parse signature of commit proof
        this.signature = rlpProof.get(1).getRLPBytes();

        this.parsed = true;
    }

    public byte[] getHash() {
        if (hashCache == null) {
            hashCache = HashUtil.sha3(getEncoded());
        }

        return hashCache;
    }

    public byte[] getEncoded() {
        if (this.rlpEncoded == null) {
            // Get encoded body
            byte[] bodyEncoded = getEncodedBody();
            if (bodyEncoded == null) {
                logger.error("Empty encoded body data");
                return null;
            }

            // Get signature of body
            if (signature == null) {
                logger.error("Empty signature data");
                return null;
            }
            byte[] signatureEncoded = RLPUtils.encodeElement(signature);

            this.rlpEncoded = RLPUtils.encodeList(bodyEncoded, signatureEncoded);
        }

        return this.rlpEncoded;
    }

    public VrfProof getVrfProof() {
        parseRLP();

        return vrfProof;
    }

    public byte[] getCoinbase() {
        parseRLP();

        return coinbase;
    }

    public BlockIdentifier getBlockIdentifier() {
        parseRLP();

        return blockIdentifier;
    }

    public byte[] getSignature() {
        parseRLP();

        return signature;
    }

    public int getPriority(int expected, long weight, long totalWeight) {
        parseRLP();

        if (vrfProof == null) {
            logger.error("Big Problem, NULL vrfProve to get VRF priority parameters");
            return 0;
        }

        return vrfProof.getPriority(expected, weight, totalWeight);
    }

    public byte[] getVrfPk() {
        if (vrfProof != null) {
            return vrfProof.getVrfPk();
        }

        return null;
    }

    public String toString() {
        return toStringWithSuffix("\n");
    }

    private String toStringWithSuffix(final String suffix) {
        parseRLP();

        StringBuilder toStringBuff = new StringBuilder();
        toStringBuff.append("  hash=").append(toHexString(getHash())).append(suffix);
        toStringBuff.append("  vrfProve=").append(vrfProof.toString()).append(suffix);
        toStringBuff.append("  coinbase=").append(toHexString(coinbase)).append(suffix);
        toStringBuff.append("  blockIdentifier=").append(blockIdentifier).append(suffix);
        toStringBuff.append("  signature=").append(toHexString(signature)).append(suffix);
        return toStringBuff.toString();
    }

    private void decodeBody(RLPList rlpBody) {
        // Parse body of commit proof
        RLPList rlpVrfProof = (RLPList) rlpBody.get(0);
        this.vrfProof = new VrfProof(rlpVrfProof);

        this.coinbase = rlpBody.get(1).getRLPBytes();

        RLPList rlpBlockIdentifier = (RLPList) rlpBody.get(2);
        this.blockIdentifier = new BlockIdentifier(rlpBlockIdentifier);
    }

    private byte[] getEncodedBody() {
        if (vrfProof == null || isNullOrZeroArray(coinbase) || blockIdentifier == null) {
            logger.error("Empty body data for encoding");
            return null;
        }

        byte[] proofBytes = vrfProof.getEncoded();

        byte[] cbBytes = RLPUtils.encodeElement(coinbase);

        byte[] identifierBytes = blockIdentifier.getEncoded();

        byte[] rlpEncoded = RLPUtils.encodeList(proofBytes, cbBytes, identifierBytes);

        return rlpEncoded;
    }

    private byte[] getSignatureSeed() {
        // Make up a encoded byte array including commit proof body
        byte[] bodyEncoded = this.getEncodedBody();

        return HashUtil.sha256(bodyEncoded);
    }

    private byte[] sign(PrivateKey sk) {
        if (sk == null) {
            logger.error("Empty security key");
            return null;
        }

        // Set public key from private key
        byte[] pk = sk.generatePublicKey().getEncoded();
        if (!Arrays.equals(pk, vrfProof.getVrfPk())) {
            logger.error("Not the same public key as VrfProof told");
            return null;
        }

        // Make up a signature of commit proof body
        byte[] seed = getSignatureSeed();
        try {
            signature = sk.sign(seed);
        } catch (CryptoException ex) {
            return null;
        }

        hashCache = null;

        return signature;
    }
    
    public int getRound() {
    	return vrfProof.getRound();
    }
}