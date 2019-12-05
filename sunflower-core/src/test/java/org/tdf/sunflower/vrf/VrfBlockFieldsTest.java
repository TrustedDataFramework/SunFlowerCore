package org.tdf.sunflower.vrf;

import org.junit.Test;
import org.tdf.common.Block;
import org.tdf.common.HexBytes;
import org.tdf.serialize.RLPDeserializer;
import org.tdf.serialize.RLPSerializer;
import org.tdf.sunflower.consensus.vrf.core.BlockIdentifier;
import org.tdf.sunflower.consensus.vrf.core.ProposalProof;
import org.tdf.sunflower.consensus.vrf.core.VrfProof;
import org.tdf.sunflower.consensus.vrf.struct.VrfBlockFields;
import org.tdf.sunflower.consensus.vrf.struct.VrfPrivateKey;
import org.tdf.sunflower.consensus.vrf.struct.VrfResult;
import org.tdf.sunflower.consensus.vrf.util.VrfUtil;
import org.tdf.sunflower.util.ByteUtil;

public class VrfBlockFieldsTest {

    public VrfBlockFieldsTest() {
        // TODO Auto-generated constructor stub
    }

    @Test
    public void testRlpNull() {
        VrfBlockFields vbf1 = VrfBlockFields.builder().nonce(null).difficulty(null).proposalProof(null).build();
        byte[] encoded = RLPSerializer.SERIALIZER.serialize(vbf1);
        VrfBlockFields vbf2 = RLPDeserializer.deserialize(encoded, VrfBlockFields.class);
        assert (ByteUtil.isNullOrZeroArray(vbf2.getNonce()));
        assert (ByteUtil.isNullOrZeroArray(vbf2.getDifficulty()));
        assert (ByteUtil.isNullOrZeroArray(vbf2.getProposalProof()));
    }

    @Test
    public void testRlpNonceDifficultyMiner() {
        String nonceStr = "abcd";
        String diffcultyStr = "cdef";
        String minerStr = "abcdef12";
        VrfBlockFields vbf1 = VrfBlockFields.builder().nonce(ByteUtil.hexStringToBytes(nonceStr))
                .difficulty(ByteUtil.hexStringToBytes(diffcultyStr)).miner(ByteUtil.hexStringToBytes(minerStr)).proposalProof(null).build();
        byte[] encoded = RLPSerializer.SERIALIZER.serialize(vbf1);
        VrfBlockFields vbf2 = RLPDeserializer.deserialize(encoded, VrfBlockFields.class);
        assert (ByteUtil.toHexString(vbf2.getNonce()).equals(nonceStr));
        assert (ByteUtil.toHexString(vbf2.getDifficulty()).equals(diffcultyStr));
        assert (ByteUtil.toHexString(vbf2.getMiner()).equals(minerStr));
        assert (ByteUtil.isNullOrZeroArray(vbf2.getProposalProof()));
    }
    
    @Test
    public void testRlpProposalProof() {
        // !!! Following hex string length should be even number.

        int round = 1;
        long blockNum = 2;
        String nonceStr = "abcd";
        String difficultyStr = "cdef";
        String blockHashStr = "abcdef";
        String minerCoinbaseStr = "0123456789abcdef";
        VrfPrivateKey vrfSk = VrfUtil.getVrfPrivateKey();
        byte[] vrfPk = vrfSk.generatePublicKey().getEncoded();

        byte[] encoded = genPayload(round, blockNum, nonceStr, difficultyStr, blockHashStr, minerCoinbaseStr, vrfSk,
                vrfPk);

        VrfBlockFields vbf2 = RLPDeserializer.deserialize(encoded, VrfBlockFields.class);
        assert (ByteUtil.toHexString(vbf2.getNonce()).equals(nonceStr));
        assert (ByteUtil.toHexString(vbf2.getDifficulty()).equals(difficultyStr));

        ProposalProof proposalProofDecoded = new ProposalProof(vbf2.getProposalProof());

        assert (proposalProofDecoded.getRound() == round);
        assert (ByteUtil.toHexString(proposalProofDecoded.getBlockIdentifier().getHash()).equals(blockHashStr));
        assert (proposalProofDecoded.getBlockIdentifier().getNumber() == blockNum);
        assert (ByteUtil.toHexString(proposalProofDecoded.getCoinbase()).equals(minerCoinbaseStr));
        assert (ByteUtil.toHexString(proposalProofDecoded.getVrfPk()).equals(ByteUtil.toHexString(vrfPk)));

    }

    private byte[] genPayload(int round, long blockNum, String nonceStr, String difficultyStr, String blockHashStr,
            String minerCoinbaseStr, VrfPrivateKey vrfSk, byte[] vrfPk) {
        VrfBlockFields vbf1 = genVrfBlockFields(round, blockNum, nonceStr, difficultyStr, blockHashStr,
                minerCoinbaseStr, vrfSk, vrfPk);

        byte[] encoded = RLPSerializer.SERIALIZER.serialize(vbf1);
        return encoded;
    }

    private VrfBlockFields genVrfBlockFields(int round, long blockNum, String nonceStr, String difficultyStr,
            String blockHashStr, String minerCoinbaseStr, VrfPrivateKey vrfSk, byte[] vrfPk) {
        byte[] nonce = ByteUtil.hexStringToBytes(nonceStr);
        byte[] difficulty = ByteUtil.hexStringToBytes(difficultyStr);

        ProposalProof proposalProof = genProposalProof(round, blockNum, blockHashStr, minerCoinbaseStr, vrfSk, vrfPk,
                nonce);

        VrfBlockFields vbf1 = VrfBlockFields.builder().nonce(nonce)
                .difficulty(difficulty).proposalProof(proposalProof.getEncoded()).miner(ByteUtil.hexStringToBytes(minerCoinbaseStr)).build();
        return vbf1;
    }

    private ProposalProof genProposalProof(int round, long blockNum, String blockHashStr, String minerCoinbaseStr,
            VrfPrivateKey vrfSk, byte[] vrfPk, byte[] nonce) {
        BlockIdentifier blockIdentifier = new BlockIdentifier(ByteUtil.hexStringToBytes(blockHashStr), blockNum);

        VrfResult vrfResult = VrfProof.Util.prove(VrfProof.ROLE_CODES_PROPOSER, round, vrfSk, nonce);
        VrfProof vrfProof = VrfProof.Util.vrfProof(VrfProof.ROLE_CODES_PROPOSER, round, vrfPk, nonce, vrfResult);

        ProposalProof proposalProof = new ProposalProof(vrfProof, ByteUtil.hexStringToBytes(minerCoinbaseStr), blockIdentifier, vrfSk.getSigner());
        return proposalProof;
    }

    @Test
    public void testBlock() {
        // !!! Following hex string length should be even number.

        int round = 1;
        long blockNum = 2;
        String nonceStr = "abcd";
        String difficultyStr = "cdef";
        String blockHashStr = "abcdef";
        String minerCoinbaseStr = "0123456789abcdef";
        VrfPrivateKey vrfSk = VrfUtil.getVrfPrivateKey();
        byte[] vrfPk = vrfSk.generatePublicKey().getEncoded();

//        byte[] encoded = genPayload(round, blockNum, nonceStr, difficultyStr, blockHashStr, minerCoinbaseStr, vrfSk,
//                vrfPk);

        VrfBlockFields vrfBlockFields = genVrfBlockFields(round, blockNum, nonceStr, difficultyStr, blockHashStr,
                minerCoinbaseStr, vrfSk, vrfPk);

        ProposalProof proposalProof = new ProposalProof(vrfBlockFields.getProposalProof());

        Block block = new Block();

        VrfUtil.setNonce(block, nonceStr);
        assert(ByteUtil.toHexString(VrfUtil.getNonce(block)).equals(nonceStr));
        VrfUtil.setDifficulty(block, difficultyStr);
        assert(ByteUtil.toHexString(VrfUtil.getDifficulty(block)).equals(difficultyStr));
        VrfUtil.setMiner(block, minerCoinbaseStr);
        assert(ByteUtil.toHexString(VrfUtil.getMiner(block)).equals(minerCoinbaseStr));
        VrfUtil.setProposalProof(block, proposalProof);

        assert(ByteUtil.toHexString(VrfUtil.getDifficulty(block)).equals(difficultyStr));
        assert(ByteUtil.toHexString(VrfUtil.getNonce(block)).equals(nonceStr));
        assert(ByteUtil.toHexString(VrfUtil.getMiner(block)).equals(minerCoinbaseStr));

        assert (proposalProof.getRound() == round);
        assert (ByteUtil.toHexString(proposalProof.getBlockIdentifier().getHash()).equals(blockHashStr));
        assert (proposalProof.getBlockIdentifier().getNumber() == blockNum);
        assert (ByteUtil.toHexString(proposalProof.getCoinbase()).equals(minerCoinbaseStr));
        assert (ByteUtil.toHexString(proposalProof.getVrfPk()).equals(ByteUtil.toHexString(vrfPk)));
    }
}
