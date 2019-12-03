package org.tdf.sunflower.consensus.vrf.core;

import org.spongycastle.util.encoders.Hex;
import org.tdf.common.Block;
import org.tdf.common.Header;
import org.tdf.sunflower.consensus.vrf.util.VrfUtil;

/**
 * @author James Hu
 * @since 2019/7/1
 */
public class VrfBlockWrapper {

	private final Block block;
	// If nodeId is empty, it tell a self mined block
	private final byte[] nodeId;

	public VrfBlockWrapper(Block block, byte[] nodeId) {
		this.block = block;
		this.nodeId = nodeId;
	}

	public Block getBlock() {
		return this.block;
	}

	public byte[] getNodeId() {
		return this.nodeId;
	}

	public Header getHeader() {
		return this.block.getHeader();
	}

	public long getNumber() {
		return this.block.getHeight();
	}

	public byte[] getCoinbase() {
		return VrfUtil.getMiner(block);
	}

	public byte[] getVdrPubkey() {
		ProposalProof proposalProof = VrfUtil.getProposalProof(block);

		if (proposalProof == null)
			return null;

		return proposalProof.getVrfPk();
	}

	public byte[] getHash() {
		return this.block.getHash().getBytes();
	}

	public String toString() {
		StringBuilder toStringBuff = new StringBuilder();
		toStringBuff.append("VrfBlkWrap {");
		toStringBuff.append("block hash=").append(Hex.toHexString(block.getHash().getBytes(), 0, 3));
		if (nodeId != null) {
			toStringBuff.append(", node id=").append(Hex.toHexString(nodeId, 0, 3));
		} else {
			toStringBuff.append(", node id=self");
		}
		toStringBuff.append("}");

		return toStringBuff.toString();
	}
}