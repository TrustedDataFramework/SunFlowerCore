package org.tdf.consortium.consensus.vrf.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

/**
 * @author James Hu
 * @since 2019/5/13
 */
public class Liveness {

    private static final Logger logger = LoggerFactory.getLogger("Liveness");

    // Different P2P node has different values, because they did not calculate values at the same time.
    // DO NOT sync Liveness data from P2P node, in case of getting faked one.
    // Every node should do liveness statistics by itself.

    private final byte[] coinbase;
    // The latest hit index to tell that validator is proposed or voted in Validator Manager.
    private long index;
    // The hits statistics data, including first start hit and sum of hit priority.
    private Hitsness hitsness;
    // The missed times to tell that validator is missed to propose or vote in Validator Manager.
    private int misses;

    public Liveness(Liveness lvn) {
        this.coinbase = lvn.coinbase;
        this.index = lvn.index;
        this.hitsness = new Hitsness(lvn.hitsness);
        this.misses = lvn.misses;
    }

    public Liveness(byte[] coinbase, long start) {
        if (start < 0) {
            throw new RuntimeException("Invalid index to record Liveness");
        }

        this.coinbase = coinbase;

        this.index = start;
        this.hitsness = new Hitsness(start, 0);

        this.misses = 0;
    }

    public Liveness(byte[] coinbase, long start, long priority) {
        if (start < 0 || priority <= 0) {
            throw new RuntimeException("Invalid index/priority to activate Liveness");
        }

        this.coinbase = coinbase;

        this.index = start;
        this.hitsness = new Hitsness(start, priority);

        this.misses = 0;
    }

    public void reset(long start, long priority) {
        if (start < 0 || priority <= 0) {
            throw new RuntimeException("Invalid index/priority to activate Liveness");
        }

        this.index = start;
        this.misses = 0;

        hitsness.reset(start, priority);
    }

    public byte[] getCoinbase() {
        return coinbase;
    }

    /**
     * Return hit index of latest one which a validator proposed or voted
     * @return The latest hit index
     */
    public long getIndex() {
        return index;
    }

    /**
     * Return missed times of latest one which a validator missed to propose or vote
     * @return The latest missed times
     */
    public int getMisses() {
        return misses;
    }

    public int stampMisses(long index, long weight, long totalWeight) {
        if (index < 0 || weight < ValidatorManager.WEIGHT_MIN_VALUE || totalWeight < weight) {
            throw new RuntimeException("Invalid input params to get hit misses");
        }

        // Get live span from start index to current index
        final long liveSpan = getLiveSpan(index);

        // We assume every hit is 1 priority, so the real total hits is less than hit count
        final long totalHits = liveSpan;

        // Get user hit cycle to check its misses
        long activeCycle = getActiveCycle(weight, totalWeight);
        // Add redundancy for hit cycle to promise we are not misjudged in case of VRF selecting deviation
        //hitCycle += hitCycle * REDUNDANCY_RATE_IN_HIT_CYCLE;
        if (liveSpan > activeCycle) {
            long hitsExpected = weight * totalHits / totalWeight;
            //if (hitsness.getHits() < hitsExpected * HIT_RATE_THRESHOLD) {
            if (hitsness.getHits() < hitsExpected) {
                ++misses;
                logger.warn("Detect a hit miss, weight {}, total weight {}, hit cycle {}, hit span {}, total hit {}, hits expected {}, hits {}",
                        weight, totalWeight, activeCycle, liveSpan, totalHits, hitsExpected, hitsness.getHits());
            }
        }

        return misses;
    }

    public void activate(long index, long priority) {
        if (index < 0 || priority <= 0) {
            throw new RuntimeException("Invalid index/priority to hit Liveness");
        }

        this.index = index;

        hitsness.hit(priority);
    }

    /**
     * Return span of hit index between liveness setup index and current index
     * @return The total hit count
     */
    public long getLiveSpan(long index) {
        // Get hit count from start index to current index
        long hitStart = hitsness.getStart();
        long hitSpan = index - hitStart + 1;
        if (hitSpan < 0) {
            // Long.MIN_VALUE == -1 - Long.MAX_VALUE;
            hitSpan += Long.MAX_VALUE + 1;
        }

        return hitSpan;
    }

    public static long getActiveCycle(long weight, long totalWeight) {
        if (weight < ValidatorManager.WEIGHT_MIN_VALUE || totalWeight < weight) {
            throw new RuntimeException("Invalid weight or total to calculate hit cycle");
        }

        // VRF algorithm promise every sub user to be selected at least once in the count of all sub users.
        //long vrfSubUserCount = (totalWeight + ValidatorManager.DEPOSIT_THRESHOLD_AS_VALIDATOR - 1) / ValidatorManager.DEPOSIT_THRESHOLD_AS_VALIDATOR;
        // Selected rate with priority is in direct proportion of weight,
        // so we get the the hit cycle as that user always got 1 priority as selected one.
        // In such hit cycle, user should be selected at least one time.
        //long hitCycle = vrfSubUserCount * ValidatorManager.DEPOSIT_THRESHOLD_AS_VALIDATOR / weight;
        long hitCycle = (totalWeight + weight - 1) / weight;

        return hitCycle;
    }

    public String toString() {
        return toStringWithSuffix("\n");
    }

    private String toStringWithSuffix(final String suffix) {
        StringBuilder toStringBuff = new StringBuilder();
        toStringBuff.append("Liveness {").append(suffix);
        toStringBuff.append("coinbase=0x").append(Hex.toHexString(coinbase, 0, 3));
        toStringBuff.append(", index=").append(index);
        toStringBuff.append(", hitsness=").append(hitsness);
        toStringBuff.append(", misses=").append(misses).append(suffix);
        toStringBuff.append("}");

        return toStringBuff.toString();
    }

    public class Hitsness {
        // The index from which start to do hits statistics.
        private long start;
        // The total priorities to tell that how many priorities accumulated from hit.
        private long hits;

        public Hitsness(Hitsness hitsness) {
            this.start = hitsness.start;
            this.hits = hitsness.hits;
        }

        public Hitsness(long start, long priority) {
            this.start = start < 0 ? 0 : start;
            this.hits = priority < 0 ? 0 : priority;
        }

        public long hit(long priority) {
            if (priority <= 0) {
                throw new RuntimeException("Invalid priority to hit Hitsness");
            }

            hits += priority;

            return hits;
        }

        public long getStart() {
            return start;
        }

        public long getHits() {
            return hits;
        }

        public void reset(long start, long priority) {
            if (start < 0 || priority < 0) {
                throw new RuntimeException("Invalid start/priority to reset hitsness");
            }

            this.start = start;
            this.hits = priority;
        }

        public String toString() {
            StringBuilder toStringBuff = new StringBuilder();
            toStringBuff.append("Hitsness {");
            toStringBuff.append("start=").append(start);
            toStringBuff.append(", hits=").append(hits);
            toStringBuff.append("}");

            return toStringBuff.toString();
        }
    }
}