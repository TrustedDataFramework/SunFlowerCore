package org.tdf.sunflower.vrf;

import org.junit.Test;
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
    public void testRlpNonceDifficulty() {
        VrfBlockFields vbf1 = VrfBlockFields.builder().nonce(ByteUtil.hexStringToBytes("abcd"))
                .difficulty(ByteUtil.hexStringToBytes("cdef")).proposalProof(null).build();
        byte[] encoded = RLPSerializer.SERIALIZER.serialize(vbf1);
        VrfBlockFields vbf2 = RLPDeserializer.deserialize(encoded, VrfBlockFields.class);
        assert (ByteUtil.toHexString(vbf2.getNonce()).equals("abcd"));
        assert (ByteUtil.toHexString(vbf2.getDifficulty()).equals("cdef"));
        assert (ByteUtil.isNullOrZeroArray(vbf2.getProposalProof()));
    }
    
    @Test
    public void testRlpProposalProof() {
        // !!! Following hex string length should be even number.
        
        int round = 1;
        String nonceStr = "abcd";
        byte[] nonce = ByteUtil.hexStringToBytes(nonceStr);
        String difficultyStr = "cdef";
        byte[] difficulty = ByteUtil.hexStringToBytes(difficultyStr);
        VrfPrivateKey vrfSk = VrfUtil.getVrfPrivateKey();
        byte[] vrfPk = vrfSk.generatePublicKey().getEncoded();
        String blockHashStr = "abcdef";
        long blockNum = 2;
        BlockIdentifier blockIdentifier = new BlockIdentifier(ByteUtil.hexStringToBytes(blockHashStr), blockNum);
        
        VrfResult vrfResult = VrfProof.Util.prove(VrfProof.ROLE_CODES_PROPOSER, round, vrfSk, nonce);
        VrfProof vrfProof = VrfProof.Util.vrfProof(VrfProof.ROLE_CODES_PROPOSER, round, vrfPk, nonce, vrfResult);
        String minerCoinbaseStr = "0123456789abcdef";
        ProposalProof proposalProof = new ProposalProof(vrfProof, ByteUtil.hexStringToBytes(minerCoinbaseStr), blockIdentifier, vrfSk.getSigner());

        
        VrfBlockFields vbf1 = VrfBlockFields.builder().nonce(nonce)
                .difficulty(difficulty).proposalProof(proposalProof.getEncoded()).build();
        
        byte[] encoded = RLPSerializer.SERIALIZER.serialize(vbf1);
        VrfBlockFields vbf2 = RLPDeserializer.deserialize(encoded, VrfBlockFields.class);
        assert (ByteUtil.toHexString(vbf2.getNonce()).equals("abcd"));
        assert (ByteUtil.toHexString(vbf2.getDifficulty()).equals("cdef"));
        
        ProposalProof proposalProofDecoded = new ProposalProof(vbf2.getProposalProof());
        
        assert (proposalProofDecoded.getRound() == round);
        assert (ByteUtil.toHexString(proposalProofDecoded.getBlockIdentifier().getHash()).equals(blockHashStr));
        assert (proposalProofDecoded.getBlockIdentifier().getNumber() == blockNum);
        assert (ByteUtil.toHexString(proposalProofDecoded.getCoinbase()).equals(minerCoinbaseStr));
        assert (ByteUtil.toHexString(proposalProofDecoded.getVrfPk()).equals(ByteUtil.toHexString(vrfPk)));

    }
}
