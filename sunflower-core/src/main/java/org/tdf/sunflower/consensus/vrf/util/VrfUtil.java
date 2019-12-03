package org.tdf.sunflower.consensus.vrf.util;

import org.tdf.common.Block;
import org.tdf.common.Header;
import org.tdf.common.HexBytes;
import org.tdf.sunflower.consensus.vrf.core.ProposalProof;
import org.tdf.sunflower.consensus.vrf.core.VrfBlockWrapper;

public class VrfUtil {

    public static byte[] getMiner(HexBytes payload) {
        return null;
    }

    public static byte[] getMiner(Header header) {
        return getMiner(header.getPayload());
    }
    
    public static byte[] getMiner(Block block) {
        return getMiner(block.getPayload());
    }
    public static ProposalProof getProposalProof(HexBytes payload) {
        return null;
    }
    
    public static ProposalProof getProposalProof(Header header) {
        return getProposalProof(header.getPayload());
    }
    
    public static ProposalProof getProposalProof(Block block) {
        return getProposalProof(block.getPayload());
    }

    public static byte[] getNonce(HexBytes payload) {
        return null;
    }
    public static byte[] getNonce(Header header) {
        return getNonce(header.getPayload());
    }
    public static byte[] getNonce(Block block) {
        return getNonce(block.getPayload());
    }

    public static byte[] getNonce(VrfBlockWrapper blockWrapper) {
        return getNonce(blockWrapper.getBlock());
    }

    public static void setNonce(Block block, byte[] nonce) {

    }
    public static void setDifficulty(Block block, byte[] difficulty) {

    }
    public static void setProposalProof(Block block, ProposalProof prosalProof) {

    }
}
