package org.tdf.sunflower.consensus.vrf.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tdf.crypto.ed25519.Ed25519;
import org.tdf.rlp.RLP;
import org.tdf.sunflower.consensus.vrf.HashUtil;
import org.tdf.sunflower.consensus.vrf.struct.VrfPrivateKey;
import org.tdf.sunflower.consensus.vrf.struct.VrfPublicKey;
import org.tdf.sunflower.consensus.vrf.struct.VrfResult;
import org.tdf.sunflower.util.ByteUtil;
import org.tdf.sunflower.util.RLPList;
import org.tdf.sunflower.util.RLPUtils;

import static org.tdf.sunflower.util.ByteUtil.isNullOrZeroArray;
import static org.tdf.sunflower.util.ByteUtil.toHexString;

/**
 * @author James Hu
 * @since 2019/5/17
 */
public class VrfProof {

    /* Role codes of VRF prove */
    public static final int ROLE_CODES_PROPOSER = 0x01;
    public static final int ROLE_CODES_REDUCTION_COMMIT = 0x11;
    public static final int ROLE_CODES_FINAL_COMMIT = 0x12;
    private static final Logger logger = LoggerFactory.getLogger("VrfProve");
    /* Role of the prove */
    @RLP(0)
    private int role;
    /*
     * VRF round for propose a new block: proposal prepare -> reduction commit ->
     * final commit | | ----------------<-------------------
     */
    @RLP(1)
    private int round;
    /* VRF public key encoded byte array */
    @RLP(2)
    private byte[] vrfPk;
    /* VRF seed */
    @RLP(3)
    private byte[] seed;
    /* VRF result */
    @RLP(4)
    private VrfResult vrfResult;
    @RLP(5)
    private byte[] rlpEncoded;
    @RLP(6)
    private boolean parsed = false;
    @RLP(7)
    private byte[] hashCache;

    public VrfProof() {

    }

    public VrfProof(int role, int round, byte[] vrfPk, byte[] seed, VrfResult vrfResult) {

        this.role = role;
        this.round = round;
        this.vrfPk = vrfPk;
        this.seed = seed;
        this.vrfResult = vrfResult;

        this.parsed = true;
    }

    public VrfProof(byte[] rlpRawData) {

        this((RLPList) RLPUtils.decode2(rlpRawData).get(0));

        this.rlpEncoded = rlpRawData;
    }

    public VrfProof(RLPList rlpProve) {
        parseRLP(rlpProve);
    }

    private static byte[] composeVrfSeed(int role, int round, byte[] seed) {
        // Compose new seed with role and round
        byte[] vrfSeed = new byte[seed.length + 4 + 4];

        System.arraycopy(seed, 0, vrfSeed, 0, seed.length);

        vrfSeed[seed.length] = (byte) ((role >> 24) & 0xFF);
        vrfSeed[seed.length + 1] = (byte) ((role >> 16) & 0xFF);
        vrfSeed[seed.length + 2] = (byte) ((role >> 8) & 0xFF);
        vrfSeed[seed.length + 3] = (byte) (role & 0xFF);

        vrfSeed[seed.length + 4] = (byte) ((round >> 24) & 0xFF);
        vrfSeed[seed.length + 5] = (byte) ((round >> 16) & 0xFF);
        vrfSeed[seed.length + 6] = (byte) ((round >> 8) & 0xFF);
        vrfSeed[seed.length + 7] = (byte) (round & 0xFF);

        return vrfSeed;
    }

    private synchronized void parseRLP() {
        if (parsed)
            return;

        RLPList params = RLPUtils.decode2(rlpEncoded);
        RLPList rlpProve = (RLPList) params.get(0);

        parseRLP(rlpProve);
    }

    private void parseRLP(RLPList rlpProve) {
        if (rlpProve == null)
            return;

        byte[] roleBytes = rlpProve.get(0).getRLPBytes();
        this.role = ByteUtil.byteArrayToInt(roleBytes);

        byte[] roundBytes = rlpProve.get(1).getRLPBytes();
        this.round = ByteUtil.byteArrayToInt(roundBytes);

        this.vrfPk = rlpProve.get(2).getRLPBytes();
        this.seed = rlpProve.get(3).getRLPBytes();

        byte[] vrfResult = rlpProve.get(4).getRLPBytes();
        this.vrfResult = new VrfResult(vrfResult);

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

            if (role < 0 || isNullOrZeroArray(vrfPk) || isNullOrZeroArray(seed) || vrfResult == null)
                return null;

            byte[] vrfRole = RLPUtils.encodeInt(this.role);

            byte[] vrfRound = RLPUtils.encodeInt(this.round);

            byte[] vrfPublicKey = RLPUtils.encodeElement(this.vrfPk);

            byte[] vrfSeed = RLPUtils.encodeElement(this.seed);

            byte[] vrfResult = RLPUtils.encodeElement(this.vrfResult.getEncoded());

            this.rlpEncoded = RLPUtils.encodeList(vrfRole, vrfRound, vrfPublicKey, vrfSeed, vrfResult);
        }

        return this.rlpEncoded;
    }

    public int getRole() {
        parseRLP();

        return role;
    }

    public int getRound() {
        parseRLP();

        return round;
    }

    public byte[] getVrfPk() {
        parseRLP();

        return vrfPk;
    }

    public byte[] getSeed() {
        parseRLP();

        return seed;
    }

    public VrfResult getVrfResult() {
        parseRLP();

        return vrfResult;
    }

    public int getPriority(int expected, long weight, long totalWeight) {
        if (expected <= 0 || weight <= 0 || totalWeight <= 0) {
            logger.error("Big Problem, Invalid VRF priority parameters [" + " expected: " + expected + ", weight: "
                    + weight + ", totalWeight:" + totalWeight + " ]");
            return 0;
        }

        parseRLP();

        if (isNullOrZeroArray(vrfPk) || isNullOrZeroArray(seed) || vrfResult == null) {
            logger.error("Big Problem, Invalid VRF priority parameters [" + " vrfPk: " + vrfPk + " seed: " + seed
                    + " vrfResult: " + vrfResult + " ]");
            return 0;
        }

        VrfPublicKey publicKey = new VrfPublicKey(vrfPk, Ed25519.getAlgorithm());

        // Compose new seed with role and round
        byte[] vrfSeed = composeVrfSeed(role, round, seed);

        int priority = publicKey.calcPriority(vrfSeed, vrfResult, expected, (int) weight, totalWeight);

        return priority;
    }

    public String toString() {
        return toStringWithSuffix("\n");
    }

    private String toStringWithSuffix(final String suffix) {
        parseRLP();

        StringBuilder toStringBuff = new StringBuilder();
        toStringBuff.append("  hash=").append(toHexString(getHash())).append(suffix);
        toStringBuff.append("  role=").append(role).append(suffix);
        toStringBuff.append("  vrfPk=").append(toHexString(vrfPk)).append(suffix);
        toStringBuff.append("  seed=").append(toHexString(seed)).append(suffix);
        toStringBuff.append("  vrfResult.r=").append(toHexString(vrfResult.getR())).append(suffix);
        toStringBuff.append("  vrfResult.proof=").append(toHexString(vrfResult.getProof()));
        return toStringBuff.toString();
    }

    static public class Util {
        /* Get a new VRF Result */
        public static VrfResult prove(int role, int round, VrfPrivateKey sk, byte[] seed) {
            // Compose new seed with role and round
            byte[] vrfSeed = composeVrfSeed(role, round, seed);

            VrfResult vrfResult = sk.rand(vrfSeed);

            return vrfResult;
        }

        /* Get a new VRF Proving data */
        public static VrfProof vrfProof(int role, int round, byte[] vrfPk, byte[] seed, VrfResult vrfResult) {
            VrfProof vrfProof = new VrfProof(role, round, vrfPk, seed, vrfResult);

            return vrfProof;
        }
    }
}