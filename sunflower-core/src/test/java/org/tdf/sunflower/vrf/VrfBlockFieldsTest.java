package org.tdf.sunflower.vrf;

import org.junit.Test;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPCodec;
import org.tdf.sunflower.consensus.vrf.core.ProposalProof;
import org.tdf.sunflower.consensus.vrf.struct.VrfBlockFields;
import org.tdf.sunflower.consensus.vrf.struct.VrfPrivateKey;
import org.tdf.sunflower.consensus.vrf.util.VrfUtil;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.util.ByteUtil;

public class VrfBlockFieldsTest {

    public VrfBlockFieldsTest() {
        // TODO Auto-generated constructor stub
    }

    @Test
    public void testRlpNull() {
        VrfBlockFields vbf1 = VrfBlockFields.builder().nonce(null).priority(null).proposalProof(null).build();
        byte[] encoded = RLPCodec.encode(vbf1);
        VrfBlockFields vbf2 = RLPCodec.decode(encoded, VrfBlockFields.class);
        assert (ByteUtil.isNullOrZeroArray(vbf2.getNonce()));
        assert (ByteUtil.isNullOrZeroArray(vbf2.getPriority()));
        assert (ByteUtil.isNullOrZeroArray(vbf2.getProposalProof()));
    }

    @Test
    public void testRlpNoncePriorityMiner() {
        String nonceStr = "abcd";
        String diffcultyStr = "cdef";
        String minerStr = "abcdef12";
        VrfBlockFields vbf1 = VrfBlockFields.builder().nonce(ByteUtil.hexStringToBytes(nonceStr))
                .priority(ByteUtil.hexStringToBytes(diffcultyStr)).miner(ByteUtil.hexStringToBytes(minerStr))
                .proposalProof(null).build();
        byte[] encoded = RLPCodec.encode(vbf1);
        VrfBlockFields vbf2 = RLPCodec.decode(encoded, VrfBlockFields.class);
        assert (ByteUtil.toHexString(vbf2.getNonce()).equals(nonceStr));
        assert (ByteUtil.toHexString(vbf2.getPriority()).equals(diffcultyStr));
        assert (ByteUtil.toHexString(vbf2.getMiner()).equals(minerStr));
        assert (ByteUtil.isNullOrZeroArray(vbf2.getProposalProof()));
    }

    @Test
    public void testRlpProposalProof() {
        // !!! Following hex string length should be even number.

        int round = 1;
        long blockNum = 2;
        String nonceStr = "abcd";
        String priorityStr = "cdef";
        String blockHashStr = "abcdef";
        String minerCoinbaseStr = "0123456789abcdef";
        VrfPrivateKey vrfSk = VrfUtil.getVrfPrivateKeyDummy();
        byte[] vrfPk = vrfSk.generatePublicKey().getEncoded();

        byte[] encoded = VrfUtil.genPayload(blockNum, round, nonceStr, minerCoinbaseStr, priorityStr, blockHashStr,
                vrfSk, vrfPk);

        VrfBlockFields vbf2 = RLPCodec.decode(encoded, VrfBlockFields.class);
        assert (ByteUtil.toHexString(vbf2.getNonce()).equals(nonceStr));
        assert (ByteUtil.toHexString(vbf2.getPriority()).equals(priorityStr));

        ProposalProof proposalProofDecoded = new ProposalProof(vbf2.getProposalProof());

        assert (proposalProofDecoded.getRound() == round);
        assert (ByteUtil.toHexString(proposalProofDecoded.getBlockIdentifier().getHash()).equals(blockHashStr));
        assert (proposalProofDecoded.getBlockIdentifier().getNumber() == blockNum);
        assert (ByteUtil.toHexString(proposalProofDecoded.getCoinbase()).equals(minerCoinbaseStr));
        assert (ByteUtil.toHexString(proposalProofDecoded.getVrfPk()).equals(ByteUtil.toHexString(vrfPk)));

    }

    @Test
    public void testBlockPayload() {
        // !!! Following hex string length should be even number.

        int round = 1;
        long blockNum = 2;
        String nonceStr = "abcd";
        String priorityStr = "cdef";
        String blockHashStr = "abcdef";
        String minerCoinbaseStr = "0123456789abcdef";
        VrfPrivateKey vrfSk = VrfUtil.getVrfPrivateKeyDummy();
        byte[] vrfPk = vrfSk.generatePublicKey().getEncoded();

        byte[] encoded = VrfUtil.genPayload(blockNum, round, nonceStr, minerCoinbaseStr, priorityStr, blockHashStr,
                vrfSk, vrfPk);
        HexBytes payload = HexBytes.fromBytes(encoded);
        Block block = new Block();
        block.setPayload(payload);

        ProposalProof proposalProof = VrfUtil.getProposalProof(block);

        assert (ByteUtil.toHexString(VrfUtil.getPriority(block)).equals(priorityStr));
        assert (ByteUtil.toHexString(VrfUtil.getNonce(block)).equals(nonceStr));
        assert (ByteUtil.toHexString(VrfUtil.getMiner(block)).equals(minerCoinbaseStr));

        assert (proposalProof.getRound() == round);
        assert (ByteUtil.toHexString(proposalProof.getBlockIdentifier().getHash()).equals(blockHashStr));
        assert (proposalProof.getBlockIdentifier().getNumber() == blockNum);
        assert (ByteUtil.toHexString(proposalProof.getCoinbase()).equals(minerCoinbaseStr));
        assert (ByteUtil.toHexString(proposalProof.getVrfPk()).equals(ByteUtil.toHexString(vrfPk)));

    }

    @Test
    public void testBlockVrfUtil() {
        // !!! Following hex string length should be even number.

        int round = 1;
        long blockNum = 2;
        String nonceStr = "abcd";
        String priorityStr = "cdef";
        String blockHashStr = "abcdef";
        String minerCoinbaseStr = "0123456789abcdef";
        VrfPrivateKey vrfSk = VrfUtil.getVrfPrivateKeyDummy();
        byte[] vrfPk = vrfSk.generatePublicKey().getEncoded();

        VrfBlockFields vrfBlockFields = VrfUtil.genVrfBlockFields(blockNum, round, nonceStr, minerCoinbaseStr,
                priorityStr, blockHashStr, vrfSk, vrfPk);

        ProposalProof proposalProof = new ProposalProof(vrfBlockFields.getProposalProof());

        Block block = new Block();

        VrfUtil.setNonce(block, nonceStr);
        assert (ByteUtil.toHexString(VrfUtil.getNonce(block)).equals(nonceStr));
        VrfUtil.setPriority(block, priorityStr);
        assert (ByteUtil.toHexString(VrfUtil.getPriority(block)).equals(priorityStr));
        VrfUtil.setMiner(block, minerCoinbaseStr);
        assert (ByteUtil.toHexString(VrfUtil.getMiner(block)).equals(minerCoinbaseStr));
        VrfUtil.setProposalProof(block, proposalProof);

        assert (ByteUtil.toHexString(VrfUtil.getPriority(block)).equals(priorityStr));
        assert (ByteUtil.toHexString(VrfUtil.getNonce(block)).equals(nonceStr));
        assert (ByteUtil.toHexString(VrfUtil.getMiner(block)).equals(minerCoinbaseStr));

        assert (proposalProof.getRound() == round);
        assert (ByteUtil.toHexString(proposalProof.getBlockIdentifier().getHash()).equals(blockHashStr));
        assert (proposalProof.getBlockIdentifier().getNumber() == blockNum);
        assert (ByteUtil.toHexString(proposalProof.getCoinbase()).equals(minerCoinbaseStr));
        assert (ByteUtil.toHexString(proposalProof.getVrfPk()).equals(ByteUtil.toHexString(vrfPk)));
    }
}
