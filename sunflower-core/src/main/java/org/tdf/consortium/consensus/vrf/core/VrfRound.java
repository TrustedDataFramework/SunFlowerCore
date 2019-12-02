package org.tdf.consortium.consensus.vrf.core;

/**
 * @author James Hu
 * @since 2019/6/22
 */
public class VrfRound implements Comparable {

	private final Object owner;

	private volatile long blockNum;
	private volatile int round;

	public VrfRound(Object owner) {
		this.owner = owner;
	}

	public void setBlockNum(Object owner, long blockNum) {
		if (this.owner == owner) {
			this.blockNum = blockNum;
		}
	}

	public void setRound(Object owner, int round) {
		if (this.owner == owner) {
			this.round = round;
		}
	}

	public void setVrfRound(Object owner, long blockNum, int round) {
		if (this.owner == owner) {
			this.blockNum = blockNum;
			this.round = round;
		}
	}

	public void setVrfRound(Object owner, VrfRound vrfRound) {
		if (this.owner == owner) {
			this.blockNum = vrfRound.blockNum;
			this.round = vrfRound.round;
		}
	}

	public long getBlockNum() {
		return this.blockNum;
	}

	public int getRound() {
		return this.round;
	}

	public void nextBlock(Object owner) {
		if (this.owner == owner) {
			blockNum++;
			round = 0;
		}
	}

	public void nextRound(Object owner) {
		if (this.owner == owner) {
			round++;
			// In case of overflow
			if (round < 0) {
				round = 0;
			}
		}
	}

	public int compareTo(Object object) {
		VrfRound to = (VrfRound)object;

		long blockNum = to.blockNum;
		int round = to.round;

		return compareTo(blockNum, round);
	}

	public int compareTo(long blockNum, int round) {
		if (this.blockNum < blockNum) {
			return -1;
		} else if (this.blockNum > blockNum) {
			return 1;
		} else {
			if (this.round < round) {
				return -1;
			} else if (this.round > round) {
				return 1;
			}
		}

		return 0;
	}

	public String toString() {
		StringBuilder toStringBuff = new StringBuilder();
		toStringBuff.append("VrfRound {");
		toStringBuff.append("blockNum=").append(blockNum);
		toStringBuff.append(", round=").append(round);
		toStringBuff.append("}");

		return toStringBuff.toString();
	}
}