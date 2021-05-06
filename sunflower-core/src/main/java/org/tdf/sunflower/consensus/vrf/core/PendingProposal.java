package org.tdf.sunflower.consensus.vrf.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.tdf.sunflower.consensus.vrf.db.HashMapDB;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertTrue;

/**
 * @author James Hu
 * @since 2019/5/20
 */
public class PendingProposal {

    private static final Logger logger = LoggerFactory.getLogger("PendingProposal");

    private final ValidatorManager validatorManager;

    /* VRF Round to tell block numer and round in VRF consensus protocol */
    private final VrfRound vrfRound;

    /**
     * A Hash Map to store Proposer Proof Objects with its coinbase.
     * NOTE:
     * Coinbase is set as key of HashMap, because new VRF keys is chosen when blockchain restart.
     * So we should collect all proposer proof identified by its coinbase.
     * Map<proposer coinbase, proposer proof>
     */
    private final HashMapDB<ProposalProof> proposalProofs = new HashMapDB<>();

    private ProposalProof bestProposalProof;
    private int bestProposerPriority;

    public PendingProposal(ValidatorManager validatorManager) {
        this.validatorManager = validatorManager;
        vrfRound = new VrfRound(this);
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
            logger.warn("The new VRF Round should not be less than old one ["
                + " new VRF Round: " + vrfRound + ", old VRF Round: " + this.vrfRound
                + " ]");
        }

        vrfRound.setVrfRound(this, newVrfRound);

        // New VRF round is coming, we reset the pending proposal
        if (bestProposerPriority > 0) {
            assertTrue(proposalProofs.keys().size() > 0);
            proposalProofs.reset();

            bestProposerPriority = 0;
            bestProposalProof = null;
        }

        assertTrue(proposalProofs.keys().size() == 0);
    }

    /**
     * Adds NEW proposer proof to the queue
     *
     * @param proposalProof New proposer proof received from peer node.
     * @return It return true if new proposer proof was added to the queue,
     * otherwise it returns false if new proof was not added to the queue.
     */
    public synchronized boolean addProposalProof(ProposalProof proposalProof) {
        if (proposalProof == null) {
            logger.error("Empty proposalProof to add, quit out");
            return false;
        }

        final BlockIdentifier blockIdentifier = proposalProof.getBlockIdentifier();
        if (blockIdentifier == null) {
            logger.error("Empty Block Identifier, quit out");
            return false;
        }

        long blockNum = blockIdentifier.getNumber();
        long round = proposalProof.getRound();
        if (blockNum != vrfRound.getBlockNum() || round != vrfRound.getRound()) {
            logger.warn("VRF Round is NOT equal, proposal proof<{}:{}> - pending<{}:{}>, skip adding",
                blockNum, round, vrfRound.getBlockNum(), vrfRound.getRound());
            return false;
        }

        // Check if it is already existed in hash map of proposer proofs
        ProposalProof coinbaseProof = proposalProofs.get(proposalProof.getCoinbase());
        if (coinbaseProof != null) {
            logger.warn("The new coming proposer proof is already added, new one {}, coinbase one {}", proposalProof, coinbaseProof);
            return false;
        }

        // Check new proof and get priority of it
        int priority = validatorManager.getPriority(proposalProof, ValidatorManager.EXPECTED_PROPOSER_THRESHOLD);
        if (priority <= 0) {
            logger.error("The priority of proposal proof is not qualified, priority {}", priority);
            return false;
        }

        logger.info("addProposalProof, Get Priority {}, weight {} / {}", priority, validatorManager.getWeight(proposalProof), validatorManager.getTotalWeight());

        // Got a new proposer proof in the same VRF round, sort the best one.
        proposalProofs.put(proposalProof.getCoinbase(), proposalProof);

        tryToUpdateBestProposal(priority, proposalProof);

        return true;
    }

    /**
     * Get best proposer proof
     *
     * @return The best proposer proof of all pending proofs.
     */
    public ProposalProof getBestPendingProposal() {
        return bestProposalProof;
    }

    /**
     * Get best proposer proof
     *
     * @return The best proposer proof of all pending proofs.
     */
    public int getBestPendingPriority() {
        return bestProposerPriority;
    }

    /**
     * Get pending proposer proof being added as pending one
     *
     * @param identifier The identifier of proposer's new block
     * @return The proposer proof being added
     */
    public ProposalProof getPendingProposalByIdentifier(BlockIdentifier identifier) {
        if (identifier == null)
            return null;

        Set<byte[]> keys = proposalProofs.keys();
        if (keys == null || keys.size() <= 0)
            return null;

        Map<byte[], ProposalProof> storage = proposalProofs.getStorage();
        Iterator<Map.Entry<byte[], ProposalProof>> iterator = storage.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<byte[], ProposalProof> entry = iterator.next();
            ProposalProof proof = entry.getValue();
            if (proof != null) {
                BlockIdentifier itrIdentifier = proof.getBlockIdentifier();
                if (itrIdentifier != null) {
                    if (Arrays.equals(itrIdentifier.getHash(), identifier.getHash())) {
                        return proof;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Get pending proposer proof being added as pending one
     *
     * @param coinbase The coinbase of proposer
     * @return The proposer proof being added
     */
    public ProposalProof getPendingProposalByCoinbase(byte[] coinbase) {
        return proposalProofs.get(coinbase);
    }

    public long getProposalSize() {
        Set<byte[]> keys = proposalProofs.keys();
        if (keys == null)
            return 0;

        return keys.size();
    }

    public ProposalProof getBestProposalProof() {
        return bestProposalProof;
    }

    public void setBestProposalProof(ProposalProof bestProposalProof) {
        this.bestProposalProof = bestProposalProof;
    }

    public int getBestProposerPriority() {
        return bestProposerPriority;
    }

    public void setBestProposerPriority(int bestProposerPriority) {
        this.bestProposerPriority = bestProposerPriority;
    }

    public HashMapDB<ProposalProof> getProposalProofs() {
        return proposalProofs;
    }

    public synchronized int getValidPriority(ProposalProof proposalProof) {
        int priority = validatorManager.getPriority(proposalProof, ValidatorManager.EXPECTED_PROPOSER_THRESHOLD);

        logger.info("getValidPriority, Get Priority {}, weight {} / {}", priority, validatorManager.getWeight(proposalProof), validatorManager.getTotalWeight());

        return priority;
    }

    private boolean tryToUpdateBestProposal(int priority, ProposalProof proposalProof) {
        // Choose the high priority
        if (priority < bestProposerPriority) {
            logger.info("New added Proposal Proof is NOT the best priority: best {} <- new {}", bestProposerPriority, priority);
            return false;
        }

        // Choose the small one of identifier as the high priority
        if (priority == bestProposerPriority) {
            final BlockIdentifier newIdentifier = proposalProof.getBlockIdentifier();
            final BlockIdentifier bestIdentifier = bestProposalProof.getBlockIdentifier();
            if (newIdentifier.getNumber() != bestIdentifier.getNumber()) {
                logger.error("!!! Fatal Error, Block Number of New Proposal Proof is different to Best Proposal Proof, new #{}, best #{}", newIdentifier.getNumber(), bestIdentifier.getNumber());
                return false;
            }

            // In the case of same block number, compare two block hash as Big Endian order
            final int compare = newIdentifier.compareTo(bestIdentifier);
            if (compare > 0) {
                logger.info("The same priority, but New proposal block hash is bigger than Best one, new hash 0x{}, best hash 0x{}", Hex.toHexString(newIdentifier.getHash(), 0, 6), Hex.toHexString(bestIdentifier.getHash(), 0, 6));
                return false;
            } else if (compare == 0) {
                logger.warn("The same priority, and We got a new proposal proof with the same block identifier, best {} <- new {}", bestProposalProof, proposalProof);
                return false;
            }
        }

        if (bestProposerPriority > 0) {
            logger.debug(">>> Best proposal is changed to new proposal, new {} : 0x{}, old {} : 0x{}", priority, Hex.toHexString(proposalProof.getBlockIdentifier().getHash(), 0, 6), bestProposerPriority, Hex.toHexString(bestProposalProof.getBlockIdentifier().getHash(), 0, 6));
        } else {
            logger.debug(">>> Best proposal is initialized as new proposal, new {} : 0x{}", priority, Hex.toHexString(proposalProof.getBlockIdentifier().getHash(), 0, 6));
        }

        bestProposerPriority = priority;
        bestProposalProof = proposalProof;

        return true;
    }
}