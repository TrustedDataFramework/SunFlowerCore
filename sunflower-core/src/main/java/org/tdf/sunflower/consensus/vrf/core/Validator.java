package org.tdf.sunflower.consensus.vrf.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tdf.sunflower.consensus.vrf.HashUtil;
import org.tdf.sunflower.util.ByteUtil;
import org.tdf.sunflower.util.RLPList;
import org.tdf.sunflower.util.RLPUtils;

import java.math.BigInteger;

import static org.tdf.sunflower.util.ByteUtil.isNullOrZeroArray;
import static org.tdf.sunflower.util.ByteUtil.toHexString;

/**
 * @author James Hu
 * @since 2019/5/13
 */
public class Validator {

    /**
     * The threshold of validator deposit(Ether units)
     * NOTE:
     * DEPOSIT_MIN_VALUE and DEPOSIT_MAX_VALUE must be a multiple of DEPOSIT_UNIT_VALUE
     */
    public static final long DEPOSIT_UNIT_VALUE = 100L;
    public static final long DEPOSIT_MIN_VALUE = DEPOSIT_UNIT_VALUE * 1000;
    public static final long DEPOSIT_MAX_VALUE = DEPOSIT_UNIT_VALUE * 10000;
    public static final int ETHER_POW_WEI = 1;
    public static final BigInteger ETHER_TO_WEI = BigInteger.valueOf(10).pow(ETHER_POW_WEI);
    private static final Logger logger = LoggerFactory.getLogger("Validator");
    /* The 160-bit address to which all fees collected from the
     * successful mining of this block be transferred; formally */
    private byte[] coinbase;
    /* The amount of balance which validator pledged for proposing new block.
     * Blockchain will confiscate the deposit if it break the rule */
    /* TODO: Now we define the unit is Ether units, should switch to Wei units as BigInteger ?
	         but that will impact performance */
    private long deposit;
    /* VRF public key encoded byte array */
    private byte[] vrfPk;

    private byte[] rlpEncoded;
    private boolean parsed = false;

    private byte[] hashCache;

    public Validator(byte[] coinbase, long deposit, byte[] vrfPk) {
        this.coinbase = coinbase;

        this.deposit = deposit > DEPOSIT_MAX_VALUE ? DEPOSIT_MAX_VALUE : deposit;

        this.vrfPk = vrfPk;

        this.parsed = true;
    }

    public Validator(byte[] rlpRawData) {
        logger.debug("new from [" + toHexString(rlpRawData) + "]");

        this.rlpEncoded = rlpRawData;
    }

    private synchronized void parseRLP() {
        if (parsed) return;

        RLPList params = RLPUtils.decode2(rlpEncoded);
        RLPList rlpValidator = (RLPList) params.get(0);

        this.coinbase = rlpValidator.get(0).getRLPBytes();

        byte[] dpBytes = rlpValidator.get(1).getRLPBytes();
        this.deposit = ByteUtil.byteArrayToLong(dpBytes);

        this.vrfPk = rlpValidator.get(2).getRLPBytes();

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

            if (isNullOrZeroArray(this.vrfPk) || isNullOrZeroArray(this.coinbase))
                return null;

            byte[] coinbase = RLPUtils.encodeElement(this.coinbase);

            byte[] deposit = RLPUtils.encodeBigInteger(BigInteger.valueOf(this.deposit));

            byte[] vrfPubkey = RLPUtils.encodeElement(this.vrfPk);

            this.rlpEncoded = RLPUtils.encodeList(coinbase, deposit, vrfPubkey);
        }

        return this.rlpEncoded;
    }

    public byte[] getCoinbase() {
        parseRLP();

        return coinbase;
    }

    public long getDeposit() {
        parseRLP();

        return deposit;
    }

    public byte[] getVrfPk() {
        parseRLP();

        return vrfPk;
    }

    public String toString() {
        return toStringWithSuffix("\n");
    }

    private String toStringWithSuffix(final String suffix) {
        parseRLP();

        StringBuilder toStringBuff = new StringBuilder();
        toStringBuff.append("  hash=").append(toHexString(getHash())).append(suffix);
        toStringBuff.append("  coinbase=").append(toHexString(coinbase)).append(suffix);
        toStringBuff.append("  deposit=").append(deposit).append(suffix);
        toStringBuff.append("  vrfPk=").append(toHexString(vrfPk)).append(suffix);

        return toStringBuff.toString();
    }
}