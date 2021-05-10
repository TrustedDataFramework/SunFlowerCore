package org.tdf.sunflower.consensus.vrf.core;

import org.tdf.rlp.RLP;
import org.tdf.sunflower.consensus.vrf.util.FastByteComparisons;
import org.tdf.sunflower.util.RLPList;
import org.tdf.sunflower.util.RLPUtils;

import java.math.BigInteger;
import java.security.InvalidParameterException;

import static org.tdf.sunflower.util.ByteUtil.*;

/**
 * Block identifier holds block hash and number <br>
 * This tuple is used in some places of the core, like by
 * {@link org.silkroad.net.eth.message.EthMessageCodes#NEW_BLOCK_HASHES} message
 * wrapper
 *
 * @author Mikhail Kalinin
 * @since 04.09.2015
 */
public class BlockIdentifier {

    /**
     * Block hash
     */
    @RLP(0)
    private byte[] blockHash;

    /**
     * Block number
     */
    @RLP(1)
    private long blockNumber;

    public BlockIdentifier() {

    }

    public BlockIdentifier(RLPList rlp) {
        this.blockHash = rlp.get(0).getRLPBytes();
        this.blockNumber = byteArrayToLong(rlp.get(1).getRLPBytes());
    }

    public BlockIdentifier(byte[] blockHash, long blockNumber) {
        this.blockHash = blockHash;
        this.blockNumber = blockNumber;
    }

    public byte[] getHash() {
        return blockHash;
    }

    public long getNumber() {
        return blockNumber;
    }

    public byte[] getEncoded() {
        byte[] hash = RLPUtils.encodeElement(this.blockHash);
        byte[] number = RLPUtils.encodeBigInteger(BigInteger.valueOf(this.blockNumber));

        return RLPUtils.encodeList(hash, number);
    }

    /**
     * Compare block hash of BlockIdentifier. If two block hash is equal, return 0;
     * If this block hash is smaller than other one, return negative; If this block
     * hash is bigger than other one, return positive;
     *
     * @author James Hu
     * @since 2019/5/20
     */
    public int compareTo(BlockIdentifier other) throws InvalidParameterException {
        if (this.blockNumber < 0 || other.blockNumber < 0) {
            throw new InvalidParameterException("Block Number is Negative");
        }

        if (this.blockNumber > other.blockNumber) {
            return 1;
        } else if (this.blockNumber < other.blockNumber) {
            return -1;
        }

        if (isNullOrZeroArray(other.blockHash) || isNullOrZeroArray(this.blockHash)) {
            throw new InvalidParameterException("Block Hash is Empty");
        }

        final int otherHashLen = other.blockHash.length;
        final int selfHashLen = this.blockHash.length;
        if (otherHashLen != selfHashLen) {
            throw new InvalidParameterException("Two Block Hash byte array have different length");
        }

        // Compare two block hash as Big Endian order
        int compare = FastByteComparisons.compareTo(this.blockHash, 0, this.blockHash.length, other.blockHash, 0,
            other.blockHash.length);

        return compare;
    }

    @Override
    public String toString() {
        return "BlockIdentifier {" + "hash=" + toHexString(blockHash).substring(0, 6) + ", number=" + blockNumber + '}';
    }
}
