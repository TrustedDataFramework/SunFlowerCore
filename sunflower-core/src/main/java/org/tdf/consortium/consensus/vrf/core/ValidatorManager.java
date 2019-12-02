package org.tdf.consortium.consensus.vrf.core;

import static org.tdf.consortium.consensus.vrf.contract.VrfContracts.DEPOSIT_CONTRACT_ADDR;
import static org.wisdom.consortium.util.ByteUtil.isNullOrZeroArray;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tdf.common.BlockRepository;
import org.tdf.consortium.consensus.vrf.contract.VrfContracts;
import org.tdf.consortium.consensus.vrf.db.ByteArrayWrapper;
import org.tdf.consortium.consensus.vrf.vm.DataWord;

/**
 * @author James Hu
 * @since 2019/5/14
 *
 * Weight is not only validator deposit value, we will add reputation factor in the future.
 *
 * Reputation is calculated using the total amount of valid work a miner or committee has contributed to the system,
 * over the regularity of that work over the entire period of time during which the system has been active.
 */
public class ValidatorManager {

    private static final Logger logger = LoggerFactory.getLogger("ValidatorManager");

    public static final int EXPECTED_PROPOSER_THRESHOLD = 26;

    /* Make sure DEPOSIT_MIN_VALUE / WEIGHT_UNIT_OF_DEPOSIT > EXPECTED_PROPOSER_THRESHOLD */
	public static final long WEIGHT_UNIT_OF_DEPOSIT = Validator.DEPOSIT_UNIT_VALUE / 100;

	public static final long WEIGHT_MIN_VALUE = Validator.DEPOSIT_MIN_VALUE / WEIGHT_UNIT_OF_DEPOSIT;

	/* The Validator Registration object for getting deposit and VRF pubkey from block chain repository */
	private final VrfContracts.ValidatorRegistration validatorRegistration;

	public ValidatorManager(BlockRepository repository) {
		validatorRegistration = new VrfContracts.ValidatorRegistration(DEPOSIT_CONTRACT_ADDR, repository);
	}

	public synchronized long getWeight(byte[] coinbase) {
		return getStorageWeight(coinbase);
	}

	public synchronized long getTotalWeight() {
		long totalWeight = getStorageValidSize();

		totalWeight = totalWeight / WEIGHT_UNIT_OF_DEPOSIT;

		return totalWeight;
	}

	public synchronized List<ByteArrayWrapper> getValidDpsOwnerList() {
		List<ByteArrayWrapper> list = new ArrayList<>();

		List<DataWord> validDpsOwnerList = validatorRegistration.getValidDpsOwnerList();
		Iterator<DataWord> iterator = validDpsOwnerList.iterator();
		while (iterator.hasNext()) {
			byte[] coinbase = iterator.next().getLast20Bytes();
			ByteArrayWrapper wrapper = new ByteArrayWrapper(coinbase);
			list.add(wrapper);
		}

		return list;
	}

	public synchronized int getValidVds() {
		return validatorRegistration.getValidVds();
	}

	public synchronized boolean exist(byte[] coinbase, byte[] vrfPk) {
		return validate(coinbase, vrfPk);
	}

	public synchronized boolean validate(ProposalProof proposalProof) {
		byte[] coinbase = proposalProof.getCoinbase();
		byte[] vrfPk = proposalProof.getVrfPk();

		return validate(coinbase, vrfPk);
	}

	public synchronized boolean validate(CommitProof commitProof) {
		byte[] coinbase = commitProof.getCoinbase();
		byte[] vrfPk = commitProof.getVrfPk();

		return validate(coinbase, vrfPk);
	}

	public synchronized boolean validate(VrfBlockWrapper newBlock) {
		byte[] coinbase = newBlock.getCoinbase();
		byte[] vrfPk = newBlock.getVdrPubkey();

		return validate(coinbase, vrfPk);
	}

	/**
	 * Return priority of proposer proof
	 * @param proof - The proposer proof sent by node
	 */
	public synchronized int getPriority(ProposalProof proof, int expected) {
		if (proof == null || expected <= 0 ) {
			logger.error("Big Problem, Invalid VRF priority parameters for Proposer Proof ["
					+ " Proof: " + proof + " expected: " + expected
					+ " ]");
			return 0;
		}

		final long totalWeight = getTotalWeight();
		if (totalWeight < WEIGHT_MIN_VALUE) {
			logger.error("Total weight does not reach WEIGHT_MIN_VALUE, {} <- {}", WEIGHT_MIN_VALUE, totalWeight);
			return 0;
		}

		// Check Block Identifier
		BlockIdentifier blockIdentifier = proof.getBlockIdentifier();
		if (blockIdentifier == null) {
			logger.error("Empty Block Identifier, throw it away");
			return 0;
		}

		// Check new proof signature
		if (!proof.verify()) {
			logger.error("Wrong signature in new Proposal Proof, throw it away");
			return 0;
		}

		// Check if proposer is the validator or not
		byte[] coinbase = proof.getCoinbase();
		if (isNullOrZeroArray(coinbase)) {
			logger.error("Empty coinbase in new Proposer Proof, throw it away");
			return 0;
		}

		long weight = getWeight(coinbase);
		int priority = proof.getPriority(expected, weight, totalWeight);
		//if (priority > 0) {
		//	hit(validator, priority);
		//}

		return priority;
	}

	/**
	 * Return priority of commit Proof
	 * @param proof The commit Proof sent by node
	 */
	public synchronized int getPriority(CommitProof proof, int expected) {
		if (proof == null || expected <= 0 ) {
			logger.error("Big Problem, Invalid VRF priority parameters for Commit Proof ["
					+ " Proof: " + proof + " expected: " + expected
					+ " ]");
			return 0;
		}

		final long totalWeight = getTotalWeight();
		if (totalWeight < WEIGHT_MIN_VALUE) {
			logger.error("Total weight does not reach WEIGHT_MIN_VALUE, {} <- {}", WEIGHT_MIN_VALUE, totalWeight);
			return 0;
		}

		// Check Block Identifier
		BlockIdentifier blockIdentifier = proof.getBlockIdentifier();
		if (blockIdentifier == null) {
			logger.error("Empty Block Identifier, throw it away");
			return 0;
		}

		// Check new proof signature
		if (!proof.verify()) {
			logger.error("Wrong signature in new Commit Proof, throw it away");
			return 0;
		}

		// Check if proposer is the validator or not
		byte[] coinbase = proof.getCoinbase();
		if (isNullOrZeroArray(coinbase)) {
			logger.error("Empty coinbase in new Commit Proof, throw it away");
			return 0;
		}

		long weight = getWeight(coinbase);
		int priority = proof.getPriority(expected, weight, totalWeight);
		//if (priority > 0) {
		//	hit(validator, priority);
		//}

		return priority;
	}

	/**
	 * Return weight of proposer Proof
	 * @param Proof The proposer Proof sent by node
	 */
	public synchronized long getWeight(ProposalProof Proof) {
		if (Proof == null) {
			logger.error("Big Problem, Invalid weight parameters, Proof NULL");
			return 0;
		}

		byte[] coinbase = Proof.getCoinbase();

		return getStorageWeight(coinbase);
	}

	/**
	 * Return weight of commit Proof
	 * @param proof The commit Proof sent by node
	 */
	public synchronized long getWeight(CommitProof proof) {
		if (proof == null) {
			logger.error("Big Problem, Invalid weight parameters, Proof NULL");
			return 0;
		}

		byte[] coinbase = proof.getCoinbase();

		return getStorageWeight(coinbase);
	}

	private Validator getValidator(byte[] coinbase) {
		// Get validator from storage firstly
		Validator validator = getStorageValidator(coinbase);

		return validator;
	}

	private long getStorageWeight(byte[] coinbase) {
		Validator validator = getStorageValidator(coinbase);
		if (validator == null)
			return 0;

		long weight = validator.getDeposit();
		weight = weight / WEIGHT_UNIT_OF_DEPOSIT;

		return weight;
	}

	private Validator getStorageValidator(byte[] coinbase) {
		if (isNullOrZeroArray(coinbase)) {
			logger.error("Empty coinbase, Fail to get validator");
			return null;
		}

		byte[] vrfPk = validatorRegistration.getPubkey(coinbase);
		BigInteger deposit = validatorRegistration.getDepositOf(coinbase);
		if (isNullOrZeroArray(vrfPk) || deposit == null) {
			logger.error("Get Empty Vrf Pubkey or Deposit from Storage, return NULL for such a invalid Validator");
			return null;
		}

		// It is WEI unit, convert it to ETH unit
		deposit = deposit.divide(Validator.ETHER_TO_WEI);

		long vdrDeposit = 0;
		try {
			vdrDeposit = deposit.longValueExact();
		} catch (java.lang.ArithmeticException ex) {
			vdrDeposit = Validator.DEPOSIT_MAX_VALUE;
		}

		if (vdrDeposit < Validator.DEPOSIT_MIN_VALUE) {
			logger.error("Storage Deposit does not reach DEPOSIT_MIN_VALUE, {} <- {}",
					Validator.DEPOSIT_MIN_VALUE, vdrDeposit);
			return null;
		}

		Validator validator = new Validator(coinbase, vdrDeposit, vrfPk);

		return validator;
	}

	private long getStorageValidSize() {
		// It is WEI unit, convert it to ETH unit
		BigInteger validSize = validatorRegistration.getValidSize();
		if (validSize == null) {
			logger.warn("Empty Valid Size of storage");
			return 0;
		}

		validSize = validSize.divide(Validator.ETHER_TO_WEI);

		long totalWeight = validSize.longValueExact();
		if (totalWeight < ValidatorManager.WEIGHT_MIN_VALUE && totalWeight > 0) {
			logger.error("Storage Valid Size does not reach DEPOSIT_MIN_VALUE, {} <- {}",
					Validator.DEPOSIT_MIN_VALUE, totalWeight);
			return 0;
		}

		return totalWeight;
	}

	private boolean validate(byte[] coinbase, byte[] vrfPk) {
		if (isNullOrZeroArray(coinbase) || isNullOrZeroArray(vrfPk))
			return false;

		Validator validator = getValidator(coinbase);
		if (validator != null) {
			if (Arrays.equals(validator.getVrfPk(), vrfPk))
				return true;
		}

		return false;
	}
}
