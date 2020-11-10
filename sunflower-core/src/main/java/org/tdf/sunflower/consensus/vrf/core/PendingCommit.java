package org.tdf.sunflower.consensus.vrf.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.tdf.sunflower.consensus.vrf.db.HashMapDB;
import org.tdf.sunflower.consensus.vrf.util.VrfUtil;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.util.RLPList;
import org.tdf.sunflower.util.RLPUtils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author James Hu
 * @since 2019/5/16
 */
public class PendingCommit {

    /* It is set as PendingCommit<R> or PendingCommit<F> according to added proof */
    // private static final Logger logger =
    // LoggerFactory.getLogger("PendingCommit");
    private final Logger logger;
    private final ValidatorManager validatorManager;
    private final PendingProposal pendingProposal;
    /* VRF Round to tell block numer and round in VRF consensus protocol */
    private VrfRound vrfRound;
    /**
     * A Hash Map to store committed Reduction/Final Proof Objects with committer's
     * coinbase. NOTE: Coinbase is set as key of HashMap, because new VRF keys is
     * chosen when blockchain restart. So we should collect all committed proof
     * identified by committer's coinbase. Map<committer coinbase, commit proof>
     */
    private HashMapDB<CommitProof> commitProofs = new HashMapDB<>();
    /**
     * Store weights in priority as block identifier be committed. NOTE: To let
     * Block Miner get best committed block identifier directly, we store encoded
     * byte array of block identifier as key. Map<block identifier encoded,
     * committed weights in priority>E
     */
    private HashMapDB<CommitWeights> commitWeights = new HashMapDB<>();
    public PendingCommit(ValidatorManager validatorManager, PendingProposal pendingProposal, String tagName) {
        this.validatorManager = validatorManager;
        this.pendingProposal = pendingProposal;

        vrfRound = new VrfRound(this);

        if (tagName == null || tagName.isEmpty()) {
            logger = LoggerFactory.getLogger("PendingCommit");
        } else {
            logger = LoggerFactory.getLogger(tagName);
        }
    }

    /**
     * Get VRF block number for managing pending proofs who is proposing new block.
     *
     * @return The VRF block number of new block.
     */
    public VrfRound getVrfRound() {
        return this.vrfRound;
    }

    /**
     * Set VRF Round for managing pending proofs who is proposing new block.
     *
     * @param newVrfRound The VRF Round of new block.
     */
    public synchronized void setVrfRound(VrfRound newVrfRound) {
        final int compared = newVrfRound.compareTo(vrfRound);
        if (compared == 0) {
            logger.warn("Same VRF round to be set, ignore");
            return;
        }

        // Require new VRF round should be bigger than old one
        if (compared < 0) {
            logger.warn("The new VRF Round should not be less than old one [" + " new VRF Round: " + vrfRound
                    + ", old VRF Round: " + this.vrfRound + " ]");
        }

        vrfRound.setVrfRound(this, newVrfRound);

        // New VRF round is coming, we reset the pending commit
        Set<byte[]> keys = commitProofs.keys();
        if (keys != null && keys.size() > 0) {
            // clear commit proof
            commitProofs.reset();

            // clear commit weight
            keys = commitWeights.keys();
            if (keys != null && keys.size() > 0)
                commitWeights.reset();
        }

        assert commitProofs.keys().size() == 0;
        assert commitWeights.keys().size() == 0;
    }

    /**
     * Adds NEW commit proof to the queue
     *
     * @param commitProof New commit proof received from pear node.
     * @return It return true if new commit proof was added to the queue, otherwise
     * it returns false if new proof was not added to the queue.
     */
    public synchronized boolean addCommitProof(CommitProof commitProof) {
        if (commitProof == null)
            return false;

        final BlockIdentifier blockIdentifier = commitProof.getBlockIdentifier();
        if (blockIdentifier == null) {
            logger.error("Empty Block Identifier, quit");
            return false;
        }

        final long blockNum = blockIdentifier.getNumber();
        final long round = commitProof.getRound();
        if (blockNum != vrfRound.getBlockNum() || round != vrfRound.getRound()) {
            logger.warn("VRF Round is NOT equal, commit proof<{}:{}> - pending<{}:{}>, skip adding", blockNum, round,
                    vrfRound.getBlockNum(), vrfRound.getRound());
            return false;
        }

        // Check if it is already existed in hash map of commit proposers
        // in case of committing weight duplicated
        CommitProof coinbaseProof = commitProofs.get(commitProof.getCoinbase());
        if (coinbaseProof != null) {
            logger.warn("The new coming commit proof is already added, new one {}, coinbase one {}", commitProof,
                    coinbaseProof);
            return false;
        }

        // Check new proof and get priority of it
        int priority = validatorManager.getPriority(commitProof, ValidatorManager.EXPECTED_PROPOSER_THRESHOLD);
        if (priority <= 0) {
            logger.warn("The priority of commit proof is not qualified");
            return false;
        }

        logger.info("addCommitProof, Get Priority {}, weight {} / {}", priority,
                validatorManager.getWeight(commitProof), validatorManager.getTotalWeight());

        // Get the proposal proof by its identifier
        ProposalProof proposalProof = pendingProposal.getPendingProposalByIdentifier(blockIdentifier);
        // This block identifier is not proposed in Pending Proposal, we can not prove
        // it as a proposed block
        if (proposalProof == null) {
            logger.error(
                    "This Block Identifier is not proposed in Pending Proposal, hash 0x{}, can not prove as a proposed block, ignore it",
                    Hex.toHexString(blockIdentifier.getHash(), 0, 3));
            return false;
        }

        // Add commit block identifier to the list
        commitProofs.put(commitProof.getCoinbase(), commitProof);

        // Get priority weights of block identifier in committing proof
        byte[] identifierEncoded = blockIdentifier.getEncoded();
        long newWeight = priority;
        CommitWeights weights = commitWeights.get(identifierEncoded);
        if (weights != null) {
            weights.weights = weights.weights + newWeight;
        } else {
            commitWeights.put(identifierEncoded, new CommitWeights(newWeight));
        }

        return true;
    }

    public CommitProof getCommitProof(Block block) {
        return commitProofs.get(VrfUtil.getMiner(block));
    }

    private BlockIdentifier getBestCommittedIdentifier() {
        Set<byte[]> keys = commitWeights.keys();
        if (keys == null || keys.size() <= 0)
            return null;

        BlockIdentifier bestIdentifier = null;
        byte[] bestIdentifierEncoded = null;
        long bestCommittedWeights = 0;
        /* Get priority weights, the value is weights multiply with priority */
        // final long totalWeights = validatorManager.getTotalWeight();
        Map<byte[], CommitWeights> storage = commitWeights.getStorage();
        Iterator<Map.Entry<byte[], CommitWeights>> iterator = storage.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<byte[], CommitWeights> entry = iterator.next();
            CommitWeights weights = entry.getValue();
            if (weights.weights > bestCommittedWeights) {
                if (bestCommittedWeights >= ValidatorManager.EXPECTED_BFT_THRESHOLD) {
                    logger.error(
                            "!!! Fatal Error, Sub Optimum ∑{} is reach 2/3 threshold when sorting Best Committed Identifier, PLEASE HELP ME",
                            bestCommittedWeights);
                }
                ;
                bestCommittedWeights = weights.weights;
                bestIdentifierEncoded = entry.getKey();
            }
        }

        if (bestIdentifierEncoded != null) {
            bestIdentifier = new BlockIdentifier((RLPList) RLPUtils.decode2(bestIdentifierEncoded).get(0));
        }

        logger.info("Best committed proposer proof: [expected threshold: {}, committed weights: {}, identifier: {}]",
                ValidatorManager.EXPECTED_PROPOSER_THRESHOLD, bestCommittedWeights, bestIdentifier);

        if (bestCommittedWeights < ValidatorManager.EXPECTED_BFT_THRESHOLD) {
            logger.warn(
                    "Commited weights does not reach the threshold, expected threshold: {}, committed weights: {}, identifier: {}",
                    ValidatorManager.EXPECTED_PROPOSER_THRESHOLD, bestCommittedWeights, bestIdentifier);

            return null;
        }

        return bestIdentifier;
    }

    /**
     * Check if new block is committed as highest weights
     *
     * @param header Header of new block to check
     * @return true if it is the best block committed as highest weights
     */
    public ProofValidationResult validateBestBlock(Header header) {
        byte[] coinbase = VrfUtil.getMiner(header);
        // Get proof by its coinbase
        ProposalProof proof = pendingProposal.getPendingProposalByCoinbase(coinbase);
        if (proof == null) {
            logger.error("The proposer proof of coinbase is not existed in pending proofs, ignore the new block");
            return ProofValidationResult.PROOF_NOT_EXISTING;
        }

        // Check identifier of new block with its proof
        if (!Arrays.equals(proof.getBlockIdentifier().getHash(), header.getHash().getBytes())) {
            logger.error("The identifier of new block is not existed in pending proofs, ignore the new block");
            return ProofValidationResult.PROOF_AND_IDENTIFIER_NOT_MATCH;
        }

        // Get priority of new block and check with best proposer priority
        int priority = validatorManager.getPriority(proof, ValidatorManager.EXPECTED_PROPOSER_THRESHOLD);
        if (priority <= 0) {
            logger.error("The priority of new block is not qualified");
            return ProofValidationResult.PRIORITY_NOT_QUALIFIED;
        }

        int bestProposalPriority = pendingProposal.getBestPendingPriority();
        if (priority < bestProposalPriority) {
            logger.error("This is NOT the best block arrived. My priority: {}, Best priority: {}", priority,
                    bestProposalPriority);
            return ProofValidationResult.NOT_BEST_BLOCK;
        }

        /* Get the best block identifier being committed as 2/3 threshold reached */
        BlockIdentifier bestIdentifier = getBestCommittedIdentifier();
        if (bestIdentifier == null) {
            logger.warn("There is no block identifier being committed as best one");
            return ProofValidationResult.NO_BEST_IDENTIFIER;
        } else if (!Arrays.equals(bestIdentifier.getHash(), proof.getBlockIdentifier().getHash())) {
            logger.error("This is NOT the best block arrived, [proposal identifier: {} -> best identifier: {}]",
                    proof.getBlockIdentifier(), bestIdentifier);
            return ProofValidationResult.PROOF_AND_BEST_IDENTIFIER_NOT_MATCH;
        }

        return ProofValidationResult.OK;
    }

    /**
     * Reaches agreement on one of these options: either agreeing on a proposed
     * block, or agreeing on an empty block.
     *
     * @return A proposed block or empty block is reached as final one
     */
    public BlockIdentifier reachBestBlockIdentifier() {
        /* a specific block or empty block */
        /* Get the best block identifier being committed as 2/3 threshold reached */
        BlockIdentifier bestIdentifier = getBestCommittedIdentifier();
        if (bestIdentifier == null) {
            logger.warn("Fail to find block identifier being committed as best one");
        }

        return bestIdentifier;
    }

    public long getCommitSize() {
        Set<byte[]> keys = commitProofs.keys();
        if (keys == null)
            return 0;

        return keys.size();
    }

    public HashMapDB<CommitProof> getCommitProofs() {
        return commitProofs;
    }

    public long getCommitWeights(BlockIdentifier blockIdentifier) {
        // Get priority weights of block identifier in committing proof
        byte[] identifierEncoded = blockIdentifier.getHash();
        CommitWeights weights = commitWeights.get(identifierEncoded);
        if (weights != null)
            return weights.weights;

        return 0;
    }

    public synchronized int getValidPriority(CommitProof commitProof) {
        return validatorManager.getPriority(commitProof, ValidatorManager.EXPECTED_PROPOSER_THRESHOLD);
    }

    /* Commit Weights is to add up all voted priority */
    private class CommitWeights {
        long weights;

        public CommitWeights(long weights) {
            this.weights = weights;
        }
    }
}