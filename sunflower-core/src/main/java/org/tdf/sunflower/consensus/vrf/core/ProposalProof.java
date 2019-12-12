package org.tdf.sunflower.consensus.vrf.core;

import static org.tdf.sunflower.util.ByteUtil.isNullOrZeroArray;
import static org.tdf.sunflower.util.ByteUtil.toHexString;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tdf.crypto.CryptoException;
import org.tdf.crypto.PrivateKey;
import org.tdf.crypto.PublicKey;
import org.tdf.crypto.ed25519.Ed25519PublicKey;
import org.tdf.rlp.RLP;
import org.tdf.rlp.RLPDecoding;
import org.tdf.rlp.RLPEncoding;
import org.tdf.sunflower.consensus.vrf.HashUtil;
import org.tdf.sunflower.util.RLPList;
import org.tdf.sunflower.util.RLPUtils;

/**
 * @author James Hu
 * @since 2019/5/9
 */
public class ProposalProof {

    private static final Logger logger = LoggerFactory.getLogger("ProposalProof");

    /* Information of proposer owner */
    /* VRF proof */
    @RLP(0)
    private VrfProof vrfProof;
    /*
     * The 160-bit address to which all fees collected from the successful mining of
     * this block be transferred; formally
     */
    @RLP(1)
    private byte[] coinbase;

    /* Identifier of proposal block */
    @RLP(2)
    private BlockIdentifier blockIdentifier;

    /* Signature of proposer proof body: {vrfProof, coinbase, blockIdentifier} */
    @RLP(3)
    private byte[] signature;
    @RLP(4)
    private byte[] rlpEncoded;
    @RLP(5)
    @RLPEncoding
    @RLPDecoding
    private boolean parsed = false;
    @RLP(6)
    private byte[] hashCache;

    public ProposalProof() {

    }

    public ProposalProof(VrfProof vrfProof, byte[] coinbase, BlockIdentifier blockIdentifier, PrivateKey sk) {

        this.vrfProof = vrfProof;

        this.coinbase = coinbase;

        this.blockIdentifier = blockIdentifier;

        if (sign(sk) == null) {
            logger.error("Fail to sign in constructor");
        }

        this.parsed = true;
    }

    public ProposalProof(RLPList rlpProof) {
        parseRLP(rlpProof);
    }

    public boolean verify() {
        byte[] vrfPk = this.vrfProof.getVrfPk();
        if (isNullOrZeroArray(vrfPk) || isNullOrZeroArray(signature)) {
            logger.error("Empty signature to verify, ProposalProof {}", this.toString());
            return false;
        }

        byte[] seed = getSignatureSeed();

        PublicKey verifier = new Ed25519PublicKey(vrfPk);

        boolean verified = verifier.verify(seed, signature);
        if (!verified) {
            logger.error("Wrong signature to verify, ProposalProof {}", this.toString());
        }

        return verified;
    }

    public ProposalProof(byte[] rlpRawData) {
        logger.debug("new from [" + toHexString(rlpRawData) + "]");

        this.rlpEncoded = rlpRawData;
        parseRLP();
    }

    public byte[] getHash() {
        if (hashCache == null) {
            hashCache = HashUtil.sha3(getEncoded());
        }

        return hashCache;
    }

    public byte[] getEncoded() {
        if (this.rlpEncoded == null) {
            byte[] bodyEncoded = getEncodedBody();
            if (bodyEncoded == null) {
                logger.error("Empty encoded body data");
                return null;
            }

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
            logger.error("Big Problem, NULL vrfProof to get VRF priority parameters");
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

    private void parseRLP(RLPList rlpProof) {
        // Parse body of proposal proof
        RLPList rlpBody = (RLPList) rlpProof.get(0);
        decodeBody(rlpBody);

        // Parse signature of proposal proof
        this.signature = rlpProof.get(1).getRLPBytes();

        this.parsed = true;
    }

    private synchronized void parseRLP() {
        if (parsed)
            return;

        RLPList params = RLPUtils.decode2(rlpEncoded);
        RLPList rlpProof = (RLPList) params.get(0);

        parseRLP(rlpProof);
    }

    private String toStringWithSuffix(final String suffix) {
        parseRLP();

        StringBuilder toStringBuff = new StringBuilder();
        toStringBuff.append("  hash=").append(toHexString(getHash())).append(suffix);
        toStringBuff.append("  vrfProof=").append(vrfProof.toString()).append(suffix);
        toStringBuff.append("  coinbase=").append(toHexString(coinbase)).append(suffix);
        toStringBuff.append("  blockIdentifier=").append(blockIdentifier).append(suffix);
        toStringBuff.append("  signature=").append(toHexString(signature)).append(suffix);
        return toStringBuff.toString();
    }

    private void decodeBody(RLPList rlpBody) {
        // Parse body of proposer proof
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
        // Make up a encoded byte array including proposer proof body
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

        // Make up a signature of proposer proof body
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