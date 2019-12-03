package org.tdf.sunflower.consensus.vrf;

import java.util.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.collections4.map.LRUMap;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.tdf.common.BlockRepository;
import org.tdf.common.Header;
import org.tdf.sunflower.consensus.vrf.core.*;
import org.tdf.sunflower.consensus.vrf.db.ByteArrayWrapper;
import org.tdf.sunflower.consensus.vrf.util.VrfUtil;

/**
 * @author James Hu
 * @since 2019/6/20
 */
public class VrfStateMachine {

	private static final Logger logger = LoggerFactory.getLogger("VrfStateMachine");

	private static final boolean DEBUG = false;

	//public static final int VALIDATOR_ACTIVATE_BLOCK_COUNT = 5;

	public static final int VRF_STATE_INIT = 0;
	public static final int VRF_STATE_PROPOSAL = 1;
	public static final int VRF_STATE_REDUCTION = 2;
	public static final int VRF_STATE_FINAL = 3;

	/**
	 * Let us calculate the p2p forwarding points in a new block transfer.
	 * <p>
	 * Assume system try to transfer a 4M bytes block in p2p network:
	 * it is (4 * 1024 * 1024) / (256 per transaction) = 16384 transactions,
	 * it is (16384 transactions) / (6 seconds) = 2730 tps.
	 * <p>
	 * Assume network is 50M bps and there are 3000 ms for p2p forwarding in a new block transfer:
	 * it is (50 / 8) / 4 = 1.5625 p2p forwarding points,
	 * it is totally 1.5625 * 4 = 6.25 p2p forwarding points.
	 * <p>
	 * Above calculated data is to tell us:
	 * If network is 50M bps and there are 3000 ms for p2p forwarding in a new block transfer,
	 * the max p2p forwarding points of system should be 6,
	 * as blow:
	 * a -> b -> c -> d -> c -> d -> e
	 * <p>
	 * We think such p2p network topology can meet Consortium Blackchain requirement:
	 * 1. 50M bps network speed
	 * 2. 2730 transaction per second
	 * 3. 6 layers of p2p network topology
	 */
	public static final int PROPOSAL_TIMEOUT = 1000;
	public static final int REDUCTION_TIMEOUT = 4000;
	public static final int FINAL_TIMEOUT = 1000;

	// Block header validator
	private VrfValidator headerValidator;

	// Current state of the VRF state machine
	private int state;
	// Current VRF round of the VRF state machine
	private final VrfRound vrfRound;

    private BlockRepository blockRepository;
	/**
	 * To filter out the blocks and proofs we have already received.
	 * Block or Proof could be sent by peers even if it was already processed by Vrf consensus.
	 * <p>
	 * Every sorted committee member will broadcast 4 kinds of data received in Vrf consensus:
	 * proposal proof, new block, reduction commit proof and final commit proof
	 * <p>
	 * Averagely, VRFs function is sorting out number of committee members as threshold specified for every role,
	 * totally number of received data is 4 * threshold.
	 * <p>
	 * We also add number of 1 threshold as redundancy to reduce impact of VRFs random attribute,
	 * so we set LRU max size to 5 * threshold.
	 */
	private final int LRU_MAX_SIZE = ValidatorManager.EXPECTED_PROPOSER_THRESHOLD * 5;
	private final Map<ByteArrayWrapper, Object> receivedBlockProofs = new LRUMap<>(LRU_MAX_SIZE);
	private final Object dummyObject = new Object();

	private final ValidatorManager validatorManager;
	private final PendingVrfState pendingVrfState;
	private final LivenessAnalyzer lvnAnalyzer;

	private final java.util.Timer timer;
	private TimerTask timerTask;

	private final List<StateMachineListener> listeners = new ArrayList<>();
	private final BlockingQueue<StateChanged> notifyStateQueue = new LinkedBlockingQueue<>();
	private Thread notifyStateThread;

	// Save winner block of this VRF round
	private VrfBlockWrapper winnerBlock;
	private VrfBlockWrapper finalBlock;

	public VrfStateMachine(ValidatorManager validatorManager, PendingVrfState pendingVrfState, VrfValidator headerValidator) {
		this.validatorManager = validatorManager;
		this.pendingVrfState = pendingVrfState;
		this.headerValidator = headerValidator;

		this.lvnAnalyzer = new LivenessAnalyzer();

		vrfRound = new VrfRound(this);

		timer = new Timer();

		state = VRF_STATE_INIT;
	}

	public synchronized int transit(int expect, java.util.Timer timer) {
		if (state == expect && this.timer == timer) {
			transit();
		} else {
			logger.error("TimerTask is expected from State<{}>, current State<{}>, ignore it", expect, state);
		}

		return state;
	}

	public boolean registered(byte[] coinbase, byte[] vrfPk) {
		return validatorManager.exist(coinbase, vrfPk);
	}

	public int getState() {
		return state;
	}

	public VrfRound getVrfRound() {
		return vrfRound;
	}

	public static String getStateName(int state) {
		String name;
		switch (state) {
			case VRF_STATE_INIT: {
				name = "INIT state";
				break;
			}
			case VRF_STATE_PROPOSAL: {
				name = "PROPOSAL state";
				break;
			}
			case VRF_STATE_REDUCTION: {
				name = "REDUCTION state";
				break;
			}
			case VRF_STATE_FINAL: {
				name = "FINAL state";
				break;
			}
			default:
				name = "UNKNOWN State";
				break;
		}

		return name;
	}

	public synchronized int start(long blockNum) {
		if (state == VRF_STATE_INIT) {
			vrfRound.setBlockNum(this, blockNum);
			vrfRound.setRound(this, 0);
			pendingVrfState.setVrfRound(vrfRound);

			logger.info("VrfStateMachine start from {}", vrfRound);

			createNotifyThread();

			transit();
		}

		return state;
	}

	public synchronized void stop() {
		state = VRF_STATE_INIT;

		winnerBlock = null;
		finalBlock = null;

		cancelTimerTask();

		vrfRound.setBlockNum(this, 0);
		vrfRound.setRound(this, 0);
		pendingVrfState.setVrfRound(vrfRound);

		destroyNotifyThread();
	}

	public synchronized void addListener(StateMachineListener newListener) {
		Iterator<StateMachineListener> it = listeners.iterator();
		while (it.hasNext()) {
			StateMachineListener listener = it.next();
			if (listener == newListener) {
				return;
			}
		}

		listeners.add(newListener);
	}

	public synchronized void removeListener(StateMachineListener delListener) {
		Iterator<StateMachineListener> it = listeners.iterator();
		while (it.hasNext()) {
			StateMachineListener listener = it.next();
			if (listener == delListener) {
				it.remove();
			}
		}
	}

	public synchronized void cleanupListeners() {
		listeners.clear();
	}

	public synchronized boolean addProof(ProposalProof proposalProof, boolean isFromPeer) {
		if (isFromPeer) {
			// Check if proof owner is a registered validator
			if (!registeredOwner(proposalProof)) {
				logger.error("Owner of new Proposal Proof is not registered in Native Deposit Contract, throw it away, coinbse {}", Hex.toHexString(proposalProof.getCoinbase(), 0, 3));
				return false;
			}

			// Check block number
			if (state != VRF_STATE_INIT) {
				long blockNum = proposalProof.getBlockIdentifier().getNumber();
				if (blockNum != vrfRound.getBlockNum()) {
					return false;
				}
			}

			// Check seed
			byte[] proofSeed = proposalProof.getVrfProof().getSeed();
			byte[] bestPendingNonce = VrfUtil.getNonce(blockRepository.getBestBlock());
			if (!ByteUtils.equals(proofSeed, bestPendingNonce)) {
				logger.error("Proposal proof seed {} not match best pending nonce {}", ByteUtils.toHexString(proofSeed), ByteUtils.toHexString(bestPendingNonce));
				return false;
			}

			// Check new proof signature
			if (!proposalProof.verify()) {
				logger.error("Wrong signature in new Proposal Proof, throw it away");
				return false;
			}
		}

		// Check if it is existed in received LRU cache
		if (!addNewProofIfNotExist(proposalProof)) {
			return false;
		}

		// Other logic is not allowed in VRF_STATE_INIT state, because we could not check priority, etc.
		// So we return true as worst case
		if (state == VRF_STATE_INIT) {
			logger.warn("Invalid addProof(P) operation in VRF_STATE_INIT state");
			return true;
		}

		final int priority = pendingVrfState.getValidPriority(proposalProof);
		if (priority <= 0) {
			logger.warn("Invalid addProof(P) operation for priority {}", priority);
			return false;
		}

		if (isFromPeer && !alignState(proposalProof)) {
			return false;
		}

		// In the same VRF round, but new proof may have less state value than my state.
		// That should be look at as normal case because of network delay,
		// and this new proof does impact consensus result, so add it anymore.
		boolean result = pendingVrfState.addProposalProof(proposalProof);
		if (state > VRF_STATE_PROPOSAL) {
			logger.warn("Delayed Proposal Proof is added in state<{}>", state);
		}

		// Try to activate liveness of coinbase
		if (result) {
			lvnAnalyzer.activate(validatorManager, proposalProof.getCoinbase(), priority);
		}

		return result;
	}

	public synchronized boolean addProof(CommitProof commitProof, boolean isFromPeer) {
		if (isFromPeer) {
			// Check if proof owner is a registered validator
			if (!registeredOwner(commitProof)) {
				logger.error("Owner of new Commit Proof is not registered in Native Deposit Contract, throw it away");
				return false;
			}

			// Check block number
			if (state != VRF_STATE_INIT) {
				long blockNum = commitProof.getBlockIdentifier().getNumber();
				if (blockNum != vrfRound.getBlockNum()) {
					return false;
				}
			}

			// Check seed
			byte[] proofSeed = commitProof.getVrfProof().getSeed();
			byte[] bestPendingNonce = VrfUtil.getNonce(blockRepository.getBestBlock().getHeader());
			if (!ByteUtils.equals(proofSeed, bestPendingNonce)) {
				logger.error("Commit proof seed {} not match best pending nonce {}", ByteUtils.toHexString(proofSeed), ByteUtils.toHexString(bestPendingNonce));
				return false;
			}


			// Check new proof signature
			if (!commitProof.verify()) {
				logger.error("Wrong signature in new Commit Proof, throw it away");
				return false;
			}
		}

		// Check if it is existed in received LRU cache
		if (!addNewProofIfNotExist(commitProof)) {
			return false;
		}

		// Other logic is not allowed in VRF_STATE_INIT state, because we could not check priority, etc.
		// So we return true as worst case
		if (state == VRF_STATE_INIT) {
			logger.warn("Invalid addProof(C) operation in VRF_STATE_INIT state");
			return true;
		}

		final int priority = pendingVrfState.getValidPriority(commitProof);
		if (priority <= 0) {
			logger.warn("Invalid addProof(C) operation for priority {}", priority);
			return false;
		}

		if (isFromPeer && !alignState(commitProof)) {
			return false;
		}

		boolean result;
		// In the same VRF round, but new proof may have less state value than my state.
		// That should be look at as normal case because of network delay,
		// but this new proof does not impact consensus result, so ignore it.
		final int role = commitProof.getVrfProof().getRole();
		if (state == VRF_STATE_REDUCTION && role == VrfProof.ROLE_CODES_REDUCTION_COMMIT) {
			result = pendingVrfState.addCommitProof(commitProof);
			//if (pendingVrfState.reachCommitBlockIdentifier() != null) {
			//	transit();
			//}
		} else if (state == VRF_STATE_FINAL && role == VrfProof.ROLE_CODES_FINAL_COMMIT) {
			result = pendingVrfState.addCommitProof(commitProof);
			//if (pendingVrfState.reachFinalBlockIdentifier() != null) {
			//	transit();
			//}
		} else {
			result = false;
			logger.warn("Delayed Commit Proof<role={}> is not added in state<{}>", role, state);
		}

		// Try to activate liveness of coinbase
		if (result) {
			lvnAnalyzer.activate(validatorManager, commitProof.getCoinbase(), priority);
		}

		return result;
	}

	public synchronized boolean addNewBlock(VrfBlockWrapper newBlock) {
		// Check if block owner is a registered validator
		if (!registeredOwner(newBlock)) {
			return false;
		}

		// TODO: check block signature ???

		// Check if it is existed in received LRU cache
		if (!addNewBlockIfNotExist(newBlock)) {
			return false;
		}

		// Other logic is not allowed in VRF_STATE_INIT state, because we could not check Vrf Round, etc.
		// So we return true as worst case
		if (state == VRF_STATE_INIT) {
			logger.warn("Invalid addNewBlock operation in VRF_STATE_INIT state");
			return true;
		}

		if (newBlock.getNumber() != vrfRound.getBlockNum()) {
			int priority = -1;
			Header header = newBlock.getHeader();
			if (header != null) {
				priority = validatorManager.getPriority(VrfUtil.getProposalProof(header), ValidatorManager.EXPECTED_PROPOSER_THRESHOLD);
			}

			logger.error("New block is NOT the same block number, vrf #{} <- new #{}, skip adding, new block hash 0x{} priority {}",
					vrfRound.getBlockNum(), newBlock.getNumber(), Hex.toHexString(newBlock.getHash(), 0, 3), priority);
			return false;
		}

		if (!pendingVrfState.validateBestProposalBlock(newBlock.getHeader())) {
			logger.info("New block is NOT the best proposal block, hash 0x{}, skip adding", Hex.toHexString(newBlock.getHash(), 0, 3));
			return false;
		}

		// Do basic check for block header
		if (!isValid(newBlock.getHeader())) {
			logger.error("Invalid block header of New block");
			return false;
		}

		// New block is identified as the best one of proposal proof,
		// Check if New block is already added as Winner block.
		if (winnerBlock != null && Arrays.equals(winnerBlock.getHash(), newBlock.getHash())) {
			logger.debug("New block is already added as Winner block");
			return false;
		}

		winnerBlock = newBlock;

		logger.info("New Block is added as Winner, #{}, hash 0x{}", winnerBlock.getNumber(), Hex.toHexString(winnerBlock.getHash(), 0, 3));

		return true;
	}

	public synchronized VrfBlockWrapper getWinnerBlock() {
		if (winnerBlock != null) {
			if (pendingVrfState.validateBestProposalBlock(winnerBlock.getHeader())) {
				return winnerBlock;
			}
		}

		return null;
	}

	public synchronized VrfBlockWrapper getFinalBlock() {
		return finalBlock;
	}

	public int getValidPriority(ProposalProof proposalProof) {
		return pendingVrfState.getValidPriority(proposalProof);
	}

	public int getValidPriority(CommitProof commitProof) {
		return pendingVrfState.getValidPriority(commitProof);
	}

	public synchronized ProposalProof getBestProposalProof() {
		return pendingVrfState.getBestPendingProposalProof();
	}

	public synchronized BlockIdentifier reachCommitBlockIdentifier() {
		return pendingVrfState.reachCommitBlockIdentifier();
	}

	private boolean addNewProofIfNotExist(ProposalProof proof) {
		return receivedBlockProofs.put(new ByteArrayWrapper(proof.getHash()), dummyObject) == null;
	}

	private boolean addNewProofIfNotExist(CommitProof proof) {
		return receivedBlockProofs.put(new ByteArrayWrapper(proof.getHash()), dummyObject) == null;
	}

	private boolean addNewBlockIfNotExist(VrfBlockWrapper newBlock) {
		return receivedBlockProofs.put(new ByteArrayWrapper(newBlock.getHash()), dummyObject) == null;
	}

	private int transit() {
		switch (state) {
			case VRF_STATE_INIT: {
				state = VRF_STATE_PROPOSAL;

				// Setup auto transit time task from new state
				cancelTimerTask();
				triggerTimeoutTransit(PROPOSAL_TIMEOUT, VRF_STATE_PROPOSAL, timer);

				addStateChanged(VRF_STATE_INIT, VRF_STATE_PROPOSAL);

				break;
			}
			case VRF_STATE_PROPOSAL: {
				state = VRF_STATE_REDUCTION;

				// Setup auto transit time task from new state
				cancelTimerTask();
				triggerTimeoutTransit(REDUCTION_TIMEOUT, VRF_STATE_REDUCTION, timer);

				addStateChanged(VRF_STATE_PROPOSAL, VRF_STATE_REDUCTION);

				break;
			}
			case VRF_STATE_REDUCTION: {
				state = VRF_STATE_FINAL;

				// Setup auto transit time task from new state
				cancelTimerTask();
				triggerTimeoutTransit(FINAL_TIMEOUT, VRF_STATE_FINAL, timer);

				addStateChanged(VRF_STATE_REDUCTION, VRF_STATE_FINAL);

				break;
			}
			case VRF_STATE_FINAL: {
				state = VRF_STATE_PROPOSAL;

				// Setup auto transit time task from new state
				cancelTimerTask();
				triggerTimeoutTransit(PROPOSAL_TIMEOUT, VRF_STATE_PROPOSAL, timer);

				// To check if all nodes reach an agreement on a final block, then update VRF round
				BlockIdentifier blockIdentifier = pendingVrfState.reachFinalBlockIdentifier();
				if (blockIdentifier != null) {
					// Check if winner is the final block, and reset winner for next round
					if (winnerBlock != null) {
						if (Arrays.equals(winnerBlock.getHash(), blockIdentifier.getHash())) {
							finalBlock = winnerBlock;

							// Try to finalize the new block, protected by try/catch
							try {
								if (!finalizeNewBlock(vrfRound, finalBlock)) {
									logger.error("Fail to finalize new block, Vrf Round {}, hash 0x{}", vrfRound, Hex.toHexString(finalBlock.getHash(), 0, 3));
									finalBlock = null;
								}
							} catch (Exception ex) {
								logger.error("Unexpected Exception: {}", ex);
								logger.error("Fail to finalize new block, Vrf Round {}, hash 0x{}", vrfRound, Hex.toHexString(finalBlock.getHash(), 0, 3));
								finalBlock = null;
							}
						} else {
							logger.error("!!! Does NOT receive the Final Block, Winner Block is NOT the final committed identifier, reach {} <- winner {}, Empty the Final Block",
									Hex.toHexString(blockIdentifier.getHash(), 0, 3), Hex.toHexString(winnerBlock.getHash(), 0, 3));
							finalBlock = null;
						}

						winnerBlock = null;
					} else {
						logger.warn("Winner Block is Empty, try to Empty the Final Block");
						finalBlock = null;
					}
				} else {
					logger.warn("Does Not reach Final Block, try to Empty the Final Block");
					finalBlock = null;
				}

				// Update Vrf Round according to new block being finalized
				if (finalBlock != null) {
					vrfRound.nextBlock(this);
				} else {
					vrfRound.nextRound(this);
				}

				// Update Vrf Round to Pending Vrf State
				pendingVrfState.setVrfRound(vrfRound);

				addStateChanged(VRF_STATE_FINAL, VRF_STATE_PROPOSAL);

				break;
			}
			default: {
				logger.error("Unknown state: {}" + state);
				break;
			}
		}

		return state;
	}

	private int forceTransit(final int newState) {
		// We should do transit even between same states, because of different VRF Round.
		// We can look at it as state reset in above case.

		// Get timeout of state for auto transit
		long timeout = 0;
		switch (newState) {
			case VRF_STATE_PROPOSAL: {
				timeout = PROPOSAL_TIMEOUT;
				break;
			}
			case VRF_STATE_REDUCTION: {
				timeout = REDUCTION_TIMEOUT;
				break;
			}
			case VRF_STATE_FINAL: {
				timeout = FINAL_TIMEOUT;
				break;
			}
			default: {
				logger.error("Force to Unknown state: {}" + state);
				break;
			}
		}

		// Allow to do state transit
		if (timeout > 0) {
			final int from = state;
			state = newState;

			// Setup auto transit time task from new state
			cancelTimerTask();
			triggerTimeoutTransit(timeout, newState, timer);

			// Notify there is state changed
			addStateChanged(from, newState);
		}

		return state;
	}

	private boolean alignState(ProposalProof proof) {
		if (state == VRF_STATE_INIT) {
			logger.warn("Invalid alignState operation in VRF_STATE_INIT state");
			return false;
		}

		// When we get a proposal proof, we try to align VRF round
		// and force state to transit to VRF_STATE_PROPOSAL

		// Check proposer proof and align the state with its proposal
		long blockNum = proof.getBlockIdentifier().getNumber();

		if (blockNum != vrfRound.getBlockNum()) {
			return false;
		}

		int round = proof.getRound();

		int compared = vrfRound.compareTo(blockNum, round);
		if (compared < 0) {
			setVrfRound(blockNum, round);

			int oldState = state;
			int newState = forceTransit(VRF_STATE_PROPOSAL);
			logger.info("Align state {} -> {} as Proposer Proof <{}> tell", oldState, newState, vrfRound);
		} else if (compared > 0) {
			logger.warn("Delayed Proposal Proof is coming to VrfRound: <{}> <- <{},{}>, ignore it",
					vrfRound, blockNum, round);
			return false;
		}

		// This is the same VRF round,
		// because Proposer Proof is happened in VRF_STATE_PROPOSAL who is the startup state,
		// we have no chance to move state forward anymore.
		return true;
	}

	private boolean alignState(CommitProof proof) {
		if (state == VRF_STATE_INIT) {
			logger.warn("Invalid alignState operation in VRF_STATE_INIT state");
			return false;
		}

		// When we get a reduction commit proof, we try to align VRF round
		// and force state to transit to VRF_STATE_REDUCTION.
		// When we get a final commit proof, we try to align VRF round
		// and force state to transit to VRF_STATE_FINAL.

		long blockNum = proof.getBlockIdentifier().getNumber();

		if (blockNum != vrfRound.getBlockNum()) {
			return false;
		}

		int round = proof.getRound();

		int compared = vrfRound.compareTo(blockNum, round);
		if (compared < 0) {
			// Update VRF Round
			setVrfRound(blockNum, round);
			// Get role to tell type of commit proof
			final int role = proof.getVrfProof().getRole();
			switch (role) {
				case VrfProof.ROLE_CODES_REDUCTION_COMMIT: {
					int oldState = state;
					int newState = forceTransit(VRF_STATE_REDUCTION);
					logger.info("Align state {} -> {} as Commit Proof <{}> tell", oldState, newState, vrfRound);
					break;
				}
				case VrfProof.ROLE_CODES_FINAL_COMMIT: {
					int oldState = state;
					int newState = forceTransit(VRF_STATE_FINAL);
					logger.info("Align state {} -> {} as Final Proof <{}> tell", oldState, newState, vrfRound);
					break;
				}
				default: {
					logger.error("Unknown Proof Role<{}>", role);
					return false;
				}
			}

			return true;
		} else if (compared > 0) {
			logger.warn("Delayed Commit Proof is coming to VrfRound: <{}> <- <{},{}>, ignore it",
					vrfRound, blockNum, round);
			return false;
		}

		// This is the same VRF round, we check and move state forward
		int newState = -1;
		final int role = proof.getVrfProof().getRole();
		switch (role) {
			case VrfProof.ROLE_CODES_REDUCTION_COMMIT: {
				newState = VRF_STATE_REDUCTION;
				break;
			}
			case VrfProof.ROLE_CODES_FINAL_COMMIT: {
				newState = VRF_STATE_FINAL;
				break;
			}
			default: {
				logger.error("Unknown Proof Role<{}>", role);
				return false;
			}
		}

		if (newState > state) {
			int oldState = state;
			newState = forceTransit(newState);
			logger.info("Align state {} -> {} as Proof Role<{}> tell", oldState, newState, proof.getVrfProof().getRole());
		}

		return true;
	}

	private void triggerTimeoutTransit(long timeout, int from, Timer timer) {
		// Create timer task and save it for cancelling timer task before submit a new one
		timerTask = new StateTransitTimerTask(from, this, timer);
		timer.schedule(timerTask, timeout);
		if (DEBUG) logger.info("TimerTask is scheduled, timeout {}, from state {}", timeout, from);
	}

	private void cancelTimerTask() {
		if (timerTask != null) {
			timerTask.cancel();
			timerTask = null;

			timer.purge();
		}
	}

	private void createNotifyThread() {
		notifyStateThread = new Thread(() -> {
			try {
				while (!Thread.currentThread().isInterrupted()) {
					StateChanged stateChanged = notifyStateQueue.take();
					if (stateChanged != null) {
						try {
							notifyStateChanged(stateChanged.getFrom(), stateChanged.getTo());
						} catch (Exception ex) {
							logger.error("Unexpected Exception: {}", ex);
						}
					}
				}
				logger.error("NotifyStateThread is interrupted, Quit from notifyStateQueue");
			} catch (InterruptedException e) {
			} catch (Exception e) {
				logger.error("Fatal error while notify state changed", e);
			}
		}, "NotifyStateThread");
		notifyStateThread.start();
	}

	private void destroyNotifyThread() {
		try {
			if (!notifyStateQueue.isEmpty()) {
				notifyStateQueue.clear();
			}

			notifyStateThread.interrupt();
			notifyStateThread.join(3 * 1000);
		} catch (Exception e) {
			logger.warn("Problems destroy Notify Thead", e);
		}
	}

	private void addStateChanged(int oldState, int newState) {
		StateChanged stateChanged = new StateChanged(oldState, newState);
		notifyStateQueue.add(stateChanged);
	}

	private void notifyStateChanged(int oldState, int newState) throws DecoderException {
		if (listeners.isEmpty()) {
			return;
		}

		Iterator<StateMachineListener> it = listeners.iterator();
		while (it.hasNext()) {
			StateMachineListener listener = it.next();
			listener.stateChanged(oldState, newState);
		}
	}

	private boolean finalizeNewBlock(VrfRound vrfRound, VrfBlockWrapper finalBlock) {
		if (listeners.isEmpty()) {
			return true;
		}

		Iterator<StateMachineListener> it = listeners.iterator();
		while (it.hasNext()) {
			StateMachineListener listener = it.next();
			if (!listener.finalizeNewBlock(vrfRound, finalBlock)) {
				return false;
			}
		}

		return true;
	}

	private void setVrfRound(long blockNum, int round) {
		vrfRound.setVrfRound(this, blockNum, round);
		pendingVrfState.setVrfRound(vrfRound);

		logger.info("Set new Vrf Round {}", vrfRound);
	}

	private boolean isValid(Header header) {
		if (headerValidator == null) {
			return true;
		}

		return headerValidator.validateAndLog(header, logger);
	}

	private boolean registeredOwner(ProposalProof proposalProof) {
		byte[] coinbase = proposalProof.getCoinbase();
		byte[] vrfPk = proposalProof.getVrfPk();

		return validatorManager.exist(coinbase, vrfPk);
	}

	private boolean registeredOwner(CommitProof commitProof) {
		byte[] coinbase = commitProof.getCoinbase();
		byte[] vrfPk = commitProof.getVrfPk();

		return validatorManager.exist(coinbase, vrfPk);
	}

	private boolean registeredOwner(VrfBlockWrapper newBlock) {
		byte[] coinbase = newBlock.getCoinbase();
		byte[] vrfPk = newBlock.getVdrPubkey();

		return validatorManager.exist(coinbase, vrfPk);
	}

	private class StateChanged {
		private final int from;
		private final int to;

		public StateChanged(int from, int to) {
			this.from = from;
			this.to = to;
		}

		public int getFrom() {
			return from;
		}

		public int getTo() {
			return to;
		}
	}
}