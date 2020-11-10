package org.tdf.sunflower.consensus.vrf.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.tdf.sunflower.consensus.vrf.db.HashMapDB;
import org.tdf.sunflower.types.Header;

import java.util.Arrays;

/**
 * During consensus protocol, we have two period: # proposal period block
 * proposer submit its proposal block to network # commit period there are two
 * stages in commit period: # committee members sort proposals and submit its
 * committed proof to network. we called it as reduction commit. # committee
 * members make final agreement and submit its final agreement to network. we
 * called it as final commit.
 *
 * @author James Hu
 * @since 2019/5/16
 */
public class PendingVrfState {

    private static final Logger logger = LoggerFactory.getLogger("PendingVrfState");

    /* VRF Round to tell block numer and round in VRF consensus protocol */
    private final VrfRound vrfRound;
    /* pending proposal submitted by block proposer */
    private final PendingProposal pendingProposal;
    /**
     * pending block proposal sorting committing submitted by committee members, we
     * called it as reduction commit
     */
    private final PendingCommit pendingReductionCommit;
    /**
     * pending final agreement committing submitted by committee members, we called
     * it as final commit
     */
    private final PendingCommit pendingFinalCommit;

    public PendingVrfState(ValidatorManager validatorManager) {
        vrfRound = new VrfRound(this);

        pendingProposal = new PendingProposal(validatorManager);

        pendingReductionCommit = new PendingCommit(validatorManager, pendingProposal, "PendingCommit<R>");

        pendingFinalCommit = new PendingCommit(validatorManager, pendingProposal, "PendingCommit<F>");
    }

    /**
     * Get VRF round for managing pending proofs who is proposing new block.
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
        vrfRound.setVrfRound(this, newVrfRound);

        pendingProposal.setVrfRound(newVrfRound);
        pendingReductionCommit.setVrfRound(newVrfRound);
        pendingFinalCommit.setVrfRound(newVrfRound);
    }

    public synchronized int getValidPriority(ProposalProof proposalProof) {
        if (proposalProof == null)
            return 0;

        // Check priority of new proof
        final int priority = pendingProposal.getValidPriority(proposalProof);

        return priority;
    }

    public synchronized int getValidPriority(CommitProof commitProof) {
        if (commitProof == null)
            return 0;

        int role = commitProof.getVrfProof().getRole();
        int priority = 0;
        // Check priority of new proof
        if (role == VrfProof.ROLE_CODES_REDUCTION_COMMIT) {
            priority = pendingReductionCommit.getValidPriority(commitProof);
        } else if (role == VrfProof.ROLE_CODES_FINAL_COMMIT) {
            priority = pendingFinalCommit.getValidPriority(commitProof);
        }

        return priority;
    }

    /**
     * Adds NEW proposer proof to the queue
     *
     * @param proposalProof New proposer proof received from pear node.
     * @return It return true if new proposer proof was added to the queue,
     * otherwise it returns false if new proof was not added to the queue.
     */
    public synchronized boolean addProposalProof(ProposalProof proposalProof) {
        VrfProof vrfProof = proposalProof.getVrfProof();
        if (vrfProof == null) {
            logger.error("Invalid proposal proof: null VRF proof");
            return false;
        }

        // Got a new proposer proof in the same VRF round, sort the best one.
        return pendingProposal.addProposalProof(proposalProof);
    }

    /**
     * Get best proposal proof
     *
     * @return The best proposal proof in all pending proposal proofs.
     */
    public ProposalProof getBestPendingProposalProof() {
        return pendingProposal.getBestPendingProposal();
    }

    /**
     * Get pending proposal proof being added as pending one
     *
     * @param identifier The identifier of new proposal block
     * @return The proposal proof being added
     */
    private ProposalProof getPendingProposalProof(BlockIdentifier identifier) {
        return pendingProposal.getPendingProposalByIdentifier(identifier);
    }

    /**
     * Adds NEW commit proof to the queue
     *
     * @param commitProof New commit proof received from pear node.
     * @return It return true if new commit proof was added to the queue, otherwise
     * it returns false if new proof was not added to the queue.
     */
    public synchronized boolean addCommitProof(CommitProof commitProof) {
        VrfProof vrfProof = commitProof.getVrfProof();
        if (vrfProof == null) {
            logger.error("Invalid commit proof: null VRF proof");
            return false;
        }

        int role = commitProof.getVrfProof().getRole();
        switch (role) {
            case VrfProof.ROLE_CODES_REDUCTION_COMMIT:
                return pendingReductionCommit.addCommitProof(commitProof);
            case VrfProof.ROLE_CODES_FINAL_COMMIT:
                return pendingFinalCommit.addCommitProof(commitProof);
            default: {
                logger.error("Unknown Role of Commit Proof: {}", role);
                return false;
            }
        }
    }

    public synchronized boolean validateBestProposalBlock(Header header) {
        if (header == null) {
            logger.error("Empty header to validate as best proposal block");
            return false;
        }

        ProposalProof bestProof = pendingProposal.getBestProposalProof();
        if (bestProof != null) {
            if (bestProof.getBlockIdentifier().getNumber() == header.getHeight()) {
                if (Arrays.equals(bestProof.getBlockIdentifier().getHash(), header.getHash().getBytes())) {
                    return true;
                } else {
                    logger.warn(
                            "Validate fail, Not the same block hash, best proposal hash 0x{} <- new hash 0x{} -----> Need to fix, should not be true.",
                            Hex.toHexString(bestProof.getBlockIdentifier().getHash(), 0, 6),
                            Hex.toHexString(header.getHash().getBytes(), 0, 6));
                    return true; // -----> Need to fix, should not be true.
                }
            } else {
                logger.warn("Validate fail, Not the same block number, best proposal #{} <- new #{}",
                        bestProof.getBlockIdentifier().getNumber(), header.getHeight());
            }
        } else {
            logger.warn("Validate fail, No best Proposal find");
        }

        return false;
    }

    /**
     * Check if new block is committed as best proposal with highest weights in
     * ROLE_CODES_REDUCTION_COMMIT stage
     *
     * @param header Header of new block to check
     * @return true if it is the best reduction committed as highest weights
     */
    public ProofValidationResult validateCommitBlock(Header header) {
        return pendingReductionCommit.validateBestBlock(header);
    }

    /**
     * Check if new block is committed as final block with highest weights in
     * ROLE_CODES_FINAL_COMMIT stage
     *
     * @param header Header of new block to check
     * @return true if it is the final block committed as highest weights
     */
    public ProofValidationResult validateFinalBlock(Header header) {
        return pendingFinalCommit.validateBestBlock(header);
    }

    public BlockIdentifier reachCommitBlockIdentifier() {
        return pendingReductionCommit.reachBestBlockIdentifier();
    }

    /**
     * Reaches agreement on one of these options: either agreeing on a proposed
     * block, or agreeing on an empty block.
     *
     * @return A proposed block or empty block is reached as final one
     */
    public BlockIdentifier reachFinalBlockIdentifier() {
        return pendingFinalCommit.reachBestBlockIdentifier();
    }

    public boolean minorityCommitted(BlockIdentifier identifier) {
        long weights = pendingReductionCommit.getCommitWeights(identifier);
        if (weights < ValidatorManager.EXPECTED_PROPOSER_THRESHOLD / 3) {
            logger.error("Reduction Committed Weights is less than 1/3 threshold, weights {}", weights);
            return true;
        }

        return false;
    }

    public long getProposalProofSize() {
        return pendingProposal.getProposalSize();
    }

    public long getProposalCommitSize() {
        return pendingReductionCommit.getCommitSize();
    }

    public long getFinalCommitSize() {
        return pendingFinalCommit.getCommitSize();
    }

    public HashMapDB<CommitProof> getReductionCommitProofs() {
        return pendingReductionCommit.getCommitProofs();
    }

    public HashMapDB<CommitProof> getFinalCommitProofs() {
        return pendingFinalCommit.getCommitProofs();
    }
}