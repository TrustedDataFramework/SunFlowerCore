package org.tdf.sunflower.consensus.vrf.util;

import org.tdf.common.Block;
import org.tdf.common.Header;
import org.tdf.common.HexBytes;
import org.tdf.crypto.ed25519.Ed25519PrivateKey;
import org.tdf.sunflower.consensus.vrf.core.ProposalProof;
import org.tdf.sunflower.consensus.vrf.core.VrfBlockWrapper;
import org.tdf.sunflower.consensus.vrf.struct.VrfPrivateKey;
import org.tdf.sunflower.util.ByteUtil;

public class VrfUtil {
    public static final String VRF_PK = "c42e9a44063bfd0956da144d6500ca05351507f55f2490b3966c78a4d7e096ca";
    public static final String VRF_SK = "065994b6ccef45a1dcabbbd77cc11638308142b8c50e08845c3f1e0eeefa8dee";

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

    //-----> Need to be implemented
    public static VrfPrivateKey getVrfPrivateKey() {
        Ed25519PrivateKey skEd25519 = new Ed25519PrivateKey(ByteUtil.hexStringToBytes(VRF_SK));
        VrfPrivateKey sk = new VrfPrivateKey(skEd25519);
        return sk;
    }

}
