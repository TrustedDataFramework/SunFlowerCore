package org.tdf.sunflower.consensus.vrf.core;

import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.tdf.sunflower.consensus.vrf.db.ByteArrayWrapper;

import java.util.*;

import static org.tdf.sunflower.util.ByteUtil.isNullOrZeroArray;

/**
 * @author James Hu
 * @since 2019/5/14
 */
@Component
public class LivenessAnalyzer {

    /**
     * A threshold of inactive hit misses
     */
    public static final int INACTIVE_VALIDATOR_HIT_MISS_THRESHOLD = 3;
    public static final int MIN_INACTIVE_VALIDATOR_CHECK_INDEX = 100;
    private static final Logger logger = LoggerFactory.getLogger("LivenessAnalyzer");
    private final Map<ByteArrayWrapper, Liveness> livenessMap;
    /* The index to tell on which one validator is proposed or committed */
    private long activeIdx;
    // The index to check inactive validators
    private long itvCheckIdx;

    public LivenessAnalyzer() {
        livenessMap = new HashMap<>();

        activeIdx = 0;
        itvCheckIdx = -1;
    }

    public synchronized boolean addLiveness(Liveness liveness) {
        return add(liveness);
    }

    public synchronized boolean removeLiveness(Liveness liveness) {
        return del(liveness);
    }

    public synchronized Liveness getLiveness(byte[] coinbase) {
        return get(coinbase);
    }

    public synchronized List<Liveness> getInactiveLiveness(ValidatorManager validatorManager) {
        final long totalWeight = validatorManager.getTotalWeight();

        return getInactiveLiveness(validatorManager, totalWeight);
    }

    public synchronized void activate(ValidatorManager validatorManager, byte[] coinbase, long priority) {
        final long totalWeight = validatorManager.getTotalWeight();

        // Set initial check index
        if (itvCheckIdx < 0) {
            itvCheckIdx = getNextItvCheckIdx(0, totalWeight);
        }

        ++activeIdx;
        // Check if activeIdx is overflow
        if (activeIdx < 0) {
            activeIdx = 0;
        }

        activate(coinbase, activeIdx, priority);

        // Try to get all inactive liveness in check index
        if (activeIdx == itvCheckIdx) {
            List<Liveness> inactiveList = getInactiveLiveness(validatorManager, totalWeight);
            for (Liveness lvn : inactiveList) {
                logger.error("!!! PLEASE HELP ME, Deposit Owner in Native Deposit Contract is NOT working now, {}", lvn);
            }

            // Set check index for next time
            itvCheckIdx = getNextItvCheckIdx(itvCheckIdx, totalWeight);
        }
    }

    private long getNextItvCheckIdx(long itvCheckIdx, long totalWeight) {
        long activeCycle = totalWeight / Validator.DEPOSIT_MIN_VALUE;
        activeCycle = activeCycle < MIN_INACTIVE_VALIDATOR_CHECK_INDEX ? MIN_INACTIVE_VALIDATOR_CHECK_INDEX : activeCycle;

        long nextItvCheckIdx = itvCheckIdx + activeCycle;
        if (nextItvCheckIdx < 0) {
            // Long.MIN_VALUE == -1 - Long.MAX_VALUE;
            nextItvCheckIdx += Long.MAX_VALUE + 1;
        }

        return nextItvCheckIdx;
    }

    private Liveness get(byte[] coinbase) {
        if (isNullOrZeroArray(coinbase)) {
            logger.error("Empty coinbase to get Liveness");
            return null;
        }

        ByteArrayWrapper cbWrapper = new ByteArrayWrapper(coinbase);
        return livenessMap.get(cbWrapper);
    }

    /**
     * Add a new liveness to analyzer
     *
     * @param liveness Liveness object to be put in
     */
    private boolean add(Liveness liveness) {
        if (liveness == null) {
            logger.error("Add an Empty liveness");
            return false;
        }

        byte[] coinbase = liveness.getCoinbase();
        if (isNullOrZeroArray(coinbase)) {
            logger.error("Empty coinbase in liveness to add");
            return false;
        }

        ByteArrayWrapper cbWrapper = new ByteArrayWrapper(coinbase);
        if (livenessMap.get(cbWrapper) != null) {
            logger.warn("Liveness for coinbase is already existed, coinbase {}", Hex.toHexString(coinbase), 0, 6);
            return false;
        }

        livenessMap.put(cbWrapper, liveness);

        return true;
    }

    /**
     * Delete a liveness from analyzer
     *
     * @param liveness Liveness to be deleted
     */
    private boolean del(Liveness liveness) {
        if (liveness == null) {
            logger.error("Empty liveness to be deleted");
            return false;
        }

        byte[] coinbase = liveness.getCoinbase();
        if (isNullOrZeroArray(coinbase)) {
            logger.error("Empty coinbase in liveness to delete");
            return false;
        }

        ByteArrayWrapper cbWrapper = new ByteArrayWrapper(coinbase);
        Liveness lvn = livenessMap.remove(cbWrapper);
        if (lvn == null) {
            logger.warn("Liveness is not existed to delete, coinbase {}", Hex.toHexString(coinbase), 0, 6);
            return false;
        }

        return true;
    }

    private Liveness activate(byte[] coinbase, long activeIdx, long priority) {
        if (isNullOrZeroArray(coinbase) || activeIdx < 0 || priority <= 0) {
            throw new RuntimeException("Invalid input params to hit liveness");
        }

        ByteArrayWrapper cbWrapper = new ByteArrayWrapper(coinbase);
        Liveness lvn = livenessMap.get(cbWrapper);

        // If liveness is not existed, create a new one for hit tracking beginning
        if (lvn == null) {
            lvn = new Liveness(coinbase, activeIdx, priority);
            livenessMap.put(cbWrapper, lvn);
        } else {
            // If liveness is existed, do reset if it is missed, or active it if it is never missed.
            int misses = lvn.getMisses();
            if (misses > 0) {
                if (misses > INACTIVE_VALIDATOR_HIT_MISS_THRESHOLD) {
                    logger.info("!!! Deposit Owner in Native Deposit Contract return to Work now, {}", lvn);
                }
                lvn.reset(activeIdx, priority);
            } else {
                lvn.activate(activeIdx, priority);
            }
        }

        return lvn;
    }

    /**
     * Try to collect status of all inactive validators from ValidatorManager
     */
    private List<Liveness> getInactiveLiveness(ValidatorManager validatorManager, long totalWeight) {
        if (validatorManager == null) {
            logger.error("Empty ValidatorManager for getInactiveLiveness");
            return null;
        }

        // Get all valid deposit owner from manager
        List<ByteArrayWrapper> validDpsOwnerList = validatorManager.getValidDpsOwnerList();

        List<Liveness> list = new ArrayList<>();
        Iterator<Map.Entry<ByteArrayWrapper, Liveness>> iterEntry = livenessMap.entrySet().iterator();
        while (iterEntry.hasNext()) {
            Map.Entry<ByteArrayWrapper, Liveness> entry = iterEntry.next();
            final ByteArrayWrapper key = entry.getKey();
            final Liveness lvn = entry.getValue();
            final byte[] coinbase = key.getData();
            final long weight = validatorManager.getWeight(coinbase);

            // If deposit of liveness is less than DEPOSIT_MIN_VALUE, it becomes invalid one, remove liveness from analyzer
            if (weight < ValidatorManager.WEIGHT_MIN_VALUE) {
                logger.info("Deposit of Validator is less than WEIGHT_MIN_VALUE, remove it from liveness analyzer, coinbase {}, deposit {}", Hex.toHexString(coinbase, 0, 3), weight);
                // Delete liveness from analyzer
                iterEntry.remove();
            } else {
                // Try to stamp hit misses according to weight and total weight
                if (lvn.getLiveSpan(activeIdx) > Liveness.getActiveCycle(weight, totalWeight)) {
                    lvn.stampMisses(activeIdx, weight, totalWeight);
                }

                if (lvn.getMisses() > INACTIVE_VALIDATOR_HIT_MISS_THRESHOLD) {
                    Liveness inactive = new Liveness(lvn);
                    list.add(inactive);
                }
            }

            // Delete existed Deposit Owner from list
            if (validDpsOwnerList.remove(key)) {
                logger.debug("Deposit Owner is already monitored in LivenessAnalyzer, coinbase {}", Hex.toHexString(key.getData(), 0, 3));
            }
            ;
        }

        // We get all valid deposit owner from manager, and remove one if it is already existed in analyzer.
        // So the rest of one in the list should be added to analyzer for monitoring from now on.
        Iterator<ByteArrayWrapper> iterDpsOwner = validDpsOwnerList.iterator();
        while (iterDpsOwner.hasNext()) {
            ByteArrayWrapper validDpsOwner = iterDpsOwner.next();
            Liveness lvn = new Liveness(validDpsOwner.getData(), activeIdx);
            // Add liveness to analyzer
            if (add(lvn)) {
                logger.info("Add new liveness from repository to monitor, validDpsOwner {}", Hex.toHexString(validDpsOwner.getData(), 0, 3));
            } else {
                logger.warn("Fail to add new liveness from repository to monitor, validDpsOwner {}", Hex.toHexString(validDpsOwner.getData(), 0, 3));
            }
            ;
        }

        return list;
    }

    private void tryToStampMisses(ValidatorManager validatorManager, Liveness liveness) {
        final long totalWeight = validatorManager.getTotalWeight();
        final long weight = validatorManager.getWeight(liveness.getCoinbase());
        // Try to stamp hit misses according to weight and total weight
        final long liveSpan = liveness.getLiveSpan(activeIdx);
        final long activeCycle = Liveness.getActiveCycle(weight, totalWeight);
        // Do it every hit cycle
        if (liveSpan % activeCycle == 0) {
            liveness.stampMisses(activeIdx, weight, totalWeight);
        }
    }
}