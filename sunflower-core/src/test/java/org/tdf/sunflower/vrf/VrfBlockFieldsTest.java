package org.tdf.sunflower.vrf;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPCodec;
import org.tdf.sunflower.consensus.poa.PoAConstants;
import org.tdf.sunflower.consensus.vrf.VrfConfig;
import org.tdf.sunflower.consensus.vrf.core.CommitProof;
import org.tdf.sunflower.consensus.vrf.core.ProposalProof;
import org.tdf.sunflower.consensus.vrf.core.VrfProof;
import org.tdf.sunflower.consensus.vrf.db.HashMapDB;
import org.tdf.sunflower.consensus.vrf.struct.VrfBlockFields;
import org.tdf.sunflower.consensus.vrf.struct.VrfPrivateKey;
import org.tdf.sunflower.consensus.vrf.util.VrfMessageCode;
import org.tdf.sunflower.consensus.vrf.util.VrfUtil;
import org.tdf.sunflower.consensus.vrf.util.VrfUtil.VrfMessageCodeAndBytes;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.util.ByteUtil;
import static org.tdf.sunflower.consensus.vrf.VrfHashPolicy.HASH_POLICY;

public class VrfBlockFieldsTest {
    private VrfConfig vrfConfig = new VrfConfig();
    int round = 1;
    long blockNum = 2;
    int version = 999;

    // !!! Following hex string length should be even number.
    String prevHashStr = "fedcba";
    HexBytes prevHash = HexBytes.fromHex(prevHashStr);
    String seedStr = "abcd";
    String priorityStr = "cdef";
    String blockHashStr = "abcdef";
    String minerCoinbaseStr = "0123456789abcdef";
    byte[] minerCoinbase = ByteUtil.hexStringToBytes(minerCoinbaseStr);
    VrfPrivateKey vrfSk = VrfUtil.getVrfPrivateKeyDummy();
    byte[] vrfPk = vrfSk.generatePublicKey().getEncoded();

    public VrfBlockFieldsTest() throws IOException {
        vrfConfig.setVrfDataDir("test");
        genCommitProofsCache();
    }

    // TODO: for string and byte array, both null and empty sequence are encoded as
    // RLP null item [0x80]
    @Test
    public void testRlpNull() {
        VrfBlockFields vbf1 = VrfBlockFields.builder().seed(null).priority(null).proposalProof(null)
                .parentReductionCommitProofs(null).parentFinalCommitProofs(null).build();
        byte[] encoded = RLPCodec.encode(vbf1);
        VrfBlockFields vbf2 = RLPCodec.decode(encoded, VrfBlockFields.class);
        assert (ByteUtil.isNullOrZeroArray(vbf2.getSeed()));
        assert (ByteUtil.isNullOrZeroArray(vbf2.getPriority()));
        assert (ByteUtil.isNullOrZeroArray(vbf2.getProposalProof()));
        assert (vbf2.getParentReductionCommitProofs() == null || vbf2.getParentReductionCommitProofs().equals(""));
        assert (vbf2.getParentFinalCommitProofs() == null || vbf2.getParentFinalCommitProofs().equals(""));
    }

    @Test
    public void testRlpSeedPriorityMiner() {
        String seedStr = "abcd";
        String diffcultyStr = "cdef";
        String minerStr = "abcdef12";
        VrfBlockFields vbf1 = VrfBlockFields.builder().seed(ByteUtil.hexStringToBytes(seedStr))
                .priority(ByteUtil.hexStringToBytes(diffcultyStr)).miner(ByteUtil.hexStringToBytes(minerStr))
                .proposalProof(null).build();
        byte[] encoded = RLPCodec.encode(vbf1);
        VrfBlockFields vbf2 = RLPCodec.decode(encoded, VrfBlockFields.class);
        assert (ByteUtil.toHexString(vbf2.getSeed()).equals(seedStr));
        assert (ByteUtil.toHexString(vbf2.getPriority()).equals(diffcultyStr));
        assert (ByteUtil.toHexString(vbf2.getMiner()).equals(minerStr));
        assert (ByteUtil.isNullOrZeroArray(vbf2.getProposalProof()));
    }

    @Test
    public void testRlpProof() throws IOException {
        byte[] encoded = VrfUtil.genPayload(blockNum, round, seedStr, minerCoinbaseStr, priorityStr, blockHashStr,
                vrfSk, vrfPk, vrfConfig);

        VrfBlockFields vbf2 = RLPCodec.decode(encoded, VrfBlockFields.class);
        assert (ByteUtil.toHexString(vbf2.getSeed()).equals(seedStr));
        assert (ByteUtil.toHexString(vbf2.getPriority()).equals(priorityStr));

        ProposalProof proposalProofDecoded = RLPCodec.decode(vbf2.getProposalProof(), ProposalProof.class);
        List<CommitProof> parentReductionCommitProofs = VrfUtil
                .parseCommitProofs(vbf2.getParentReductionCommitProofs());
        CommitProof parentReductionCommitProofDecoded = parentReductionCommitProofs.get(0);

        List<CommitProof> parentFinalCommitProofs = VrfUtil.parseCommitProofs(vbf2.getParentFinalCommitProofs());
        CommitProof parentFinalCommitProofDecoded = parentFinalCommitProofs.get(0);

        assert (proposalProofDecoded.getRound() == round);
        assert (ByteUtil.toHexString(proposalProofDecoded.getBlockIdentifier().getHash()).equals(blockHashStr));
        assert (proposalProofDecoded.getBlockIdentifier().getNumber() == blockNum);
        assert (ByteUtil.toHexString(proposalProofDecoded.getCoinbase()).equals(minerCoinbaseStr));
        assert (ByteUtil.toHexString(proposalProofDecoded.getVrfPk()).equals(ByteUtil.toHexString(vrfPk)));

        assert (parentReductionCommitProofDecoded.getRound() == round);
        assert (ByteUtil.toHexString(parentReductionCommitProofDecoded.getBlockIdentifier().getHash())
                .equals(blockHashStr));
        assert (parentReductionCommitProofDecoded.getBlockIdentifier().getNumber() == blockNum);
        assert (ByteUtil.toHexString(parentReductionCommitProofDecoded.getCoinbase()).equals(minerCoinbaseStr));
        assert (ByteUtil.toHexString(parentReductionCommitProofDecoded.getVrfPk()).equals(ByteUtil.toHexString(vrfPk)));

        assert (parentFinalCommitProofDecoded.getRound() == round);
        assert (ByteUtil.toHexString(parentFinalCommitProofDecoded.getBlockIdentifier().getHash())
                .equals(blockHashStr));
        assert (parentFinalCommitProofDecoded.getBlockIdentifier().getNumber() == blockNum);
        assert (ByteUtil.toHexString(parentFinalCommitProofDecoded.getCoinbase()).equals(minerCoinbaseStr));
        assert (ByteUtil.toHexString(parentFinalCommitProofDecoded.getVrfPk()).equals(ByteUtil.toHexString(vrfPk)));

    }

    @Test
    public void testBlockPayload() throws IOException {

        byte[] encoded = VrfUtil.genPayload(blockNum, round, seedStr, minerCoinbaseStr, priorityStr, blockHashStr,
                vrfSk, vrfPk, vrfConfig);
        HexBytes payload = HexBytes.fromBytes(encoded);
        Block block = new Block();
        block.setPayload(payload);

        // Get proofs from block.
        ProposalProof proposalProof = VrfUtil.getProposalProof(block);
        List<CommitProof> parentReductionCommitProofs = VrfUtil.getParentReductionCommitProofList(block);
        CommitProof parentReductionCommitProofDecoded = parentReductionCommitProofs.get(0);
        List<CommitProof> parentFinalCommitProofs = VrfUtil.getParentFinalCommitProofList(block);
        CommitProof parentFinalCommitProofDecoded = parentFinalCommitProofs.get(0);

        assert (ByteUtil.toHexString(VrfUtil.getPriority(block)).equals(priorityStr));
        assert (ByteUtil.toHexString(VrfUtil.getSeed(block)).equals(seedStr));
        assert (ByteUtil.toHexString(VrfUtil.getMiner(block)).equals(minerCoinbaseStr));

        assert (proposalProof.getRound() == round);
        assert (ByteUtil.toHexString(proposalProof.getBlockIdentifier().getHash()).equals(blockHashStr));
        assert (proposalProof.getBlockIdentifier().getNumber() == blockNum);
        assert (ByteUtil.toHexString(proposalProof.getCoinbase()).equals(minerCoinbaseStr));
        assert (ByteUtil.toHexString(proposalProof.getVrfPk()).equals(ByteUtil.toHexString(vrfPk)));

        assert (parentReductionCommitProofDecoded.getRound() == round);
        assert (ByteUtil.toHexString(parentReductionCommitProofDecoded.getBlockIdentifier().getHash())
                .equals(blockHashStr));
        assert (parentReductionCommitProofDecoded.getBlockIdentifier().getNumber() == blockNum);
        assert (ByteUtil.toHexString(parentReductionCommitProofDecoded.getCoinbase()).equals(minerCoinbaseStr));
        assert (ByteUtil.toHexString(parentReductionCommitProofDecoded.getVrfPk()).equals(ByteUtil.toHexString(vrfPk)));

        assert (parentFinalCommitProofDecoded.getRound() == round);
        assert (ByteUtil.toHexString(parentFinalCommitProofDecoded.getBlockIdentifier().getHash())
                .equals(blockHashStr));
        assert (parentFinalCommitProofDecoded.getBlockIdentifier().getNumber() == blockNum);
        assert (ByteUtil.toHexString(parentFinalCommitProofDecoded.getCoinbase()).equals(minerCoinbaseStr));
        assert (ByteUtil.toHexString(parentFinalCommitProofDecoded.getVrfPk()).equals(ByteUtil.toHexString(vrfPk)));
    }

    @Test
    public void testBlockVrfUtil() throws IOException {
        VrfBlockFields vrfBlockFields = VrfUtil.genVrfBlockFields(blockNum, round, seedStr, minerCoinbaseStr,
                priorityStr, blockHashStr, vrfSk, vrfPk, vrfConfig);

        // Create proofs
        ProposalProof proposalProof = RLPCodec.decode(vrfBlockFields.getProposalProof(), ProposalProof.class);
        List<CommitProof> parentReductionCommitProofList = VrfUtil
                .parseCommitProofs(VrfUtil.readParentReductionCommitProofs(vrfConfig));
        List<CommitProof> parentFinalCommitProofList = VrfUtil
                .parseCommitProofs(VrfUtil.readParentFinalCommitProofs(vrfConfig));

        Block block = new Block();

        VrfUtil.setSeed(block, seedStr);
        assert (ByteUtil.toHexString(VrfUtil.getSeed(block)).equals(seedStr));
        VrfUtil.setPriority(block, priorityStr);
        assert (ByteUtil.toHexString(VrfUtil.getPriority(block)).equals(priorityStr));
        VrfUtil.setMiner(block, minerCoinbaseStr);
        assert (ByteUtil.toHexString(VrfUtil.getMiner(block)).equals(minerCoinbaseStr));

        // Set proofs to block
        VrfUtil.setProposalProof(block, proposalProof);
        VrfUtil.setParentReductionCommitProofs(block,
                VrfUtil.commitProofListToCommaSepStr(parentReductionCommitProofList));
        VrfUtil.setParentFinalCommitProofs(block, VrfUtil.commitProofListToCommaSepStr(parentFinalCommitProofList));

        assert (ByteUtil.toHexString(VrfUtil.getPriority(block)).equals(priorityStr));
        assert (ByteUtil.toHexString(VrfUtil.getSeed(block)).equals(seedStr));
        assert (ByteUtil.toHexString(VrfUtil.getMiner(block)).equals(minerCoinbaseStr));

        // Get proofs from block
        ProposalProof proposalProof2 = VrfUtil.getProposalProof(block);
        List<CommitProof> parentReductionCommitProofs2 = VrfUtil.getParentReductionCommitProofList(block);
        CommitProof parentReductionCommitProof2 = parentReductionCommitProofs2.get(0);
        List<CommitProof> parentFinalCommitProofs2 = VrfUtil.getParentFinalCommitProofList(block);
        CommitProof parentFinalCommitProof2 = parentFinalCommitProofs2.get(0);

        assert (proposalProof2.getRound() == round);
        assert (ByteUtil.toHexString(proposalProof2.getBlockIdentifier().getHash()).equals(blockHashStr));
        assert (proposalProof2.getBlockIdentifier().getNumber() == blockNum);
        assert (ByteUtil.toHexString(proposalProof2.getCoinbase()).equals(minerCoinbaseStr));
        assert (ByteUtil.toHexString(proposalProof2.getVrfPk()).equals(ByteUtil.toHexString(vrfPk)));
        assert (parentReductionCommitProof2.getRound() == round);
        assert (ByteUtil.toHexString(parentReductionCommitProof2.getBlockIdentifier().getHash()).equals(blockHashStr));
        assert (parentReductionCommitProof2.getBlockIdentifier().getNumber() == blockNum);
        assert (ByteUtil.toHexString(parentReductionCommitProof2.getCoinbase()).equals(minerCoinbaseStr));
        assert (ByteUtil.toHexString(parentReductionCommitProof2.getVrfPk()).equals(ByteUtil.toHexString(vrfPk)));

        assert (parentFinalCommitProof2.getRound() == round);
        assert (ByteUtil.toHexString(parentFinalCommitProof2.getBlockIdentifier().getHash()).equals(blockHashStr));
        assert (parentFinalCommitProof2.getBlockIdentifier().getNumber() == blockNum);
        assert (ByteUtil.toHexString(parentFinalCommitProof2.getCoinbase()).equals(minerCoinbaseStr));
        assert (ByteUtil.toHexString(parentFinalCommitProof2.getVrfPk()).equals(ByteUtil.toHexString(vrfPk)));
    }

    @Test
    public void testCommitProofRlp() {
        // !!! Following hex string length should be even number.
        CommitProof commitProof = VrfUtil.genCommitlProof(blockNum, round, seedStr, minerCoinbaseStr, blockHashStr,
                vrfSk, vrfPk, VrfProof.ROLE_CODES_REDUCTION_COMMIT);

        byte[] encoded = RLPCodec.encode(commitProof);

        CommitProof commitProof2 = RLPCodec.decode(encoded, CommitProof.class);
        assert (ByteUtil.toHexString(commitProof2.getVrfProof().getSeed()).equals(seedStr));
        assert (ByteUtil.toHexString(commitProof2.getCoinbase()).equals(minerCoinbaseStr));
        assert (commitProof2.getRound() == round);
        assert (ByteUtil.toHexString(commitProof2.getBlockIdentifier().getHash()).equals(blockHashStr));
        assert (commitProof2.getBlockIdentifier().getNumber() == blockNum);
        assert (ByteUtil.toHexString(commitProof2.getCoinbase()).equals(minerCoinbaseStr));
        assert (ByteUtil.toHexString(commitProof2.getVrfPk()).equals(ByteUtil.toHexString(vrfPk)));
    }

    @Test
    public void testCommitProofStorage() throws Exception {
        // !!! Following hex string length should be even number.

        CommitProof commitProof = VrfUtil.genCommitlProof(blockNum, round, seedStr, minerCoinbaseStr, blockHashStr,
                vrfSk, vrfPk, VrfProof.ROLE_CODES_REDUCTION_COMMIT);

        File tmpDir = new File("tmp");
        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }

        String filePath = tmpDir.getAbsolutePath() + File.separator + "proof.txt";
        HashMapDB<CommitProof> commitProofs = new HashMapDB<CommitProof>();
        commitProofs.put(minerCoinbase, commitProof);
        VrfUtil.writeCommitProofsToFile(commitProofs, filePath);

        List<CommitProof> commitProofs2 = VrfUtil.parseCommitProofs(VrfUtil.readCommitProofsFromFile(filePath));
        CommitProof commitProof2 = commitProofs2.get(0);
        assert (ByteUtil.toHexString(commitProof2.getVrfProof().getSeed()).equals(seedStr));
        assert (ByteUtil.toHexString(commitProof2.getCoinbase()).equals(minerCoinbaseStr));
        assert (commitProof2.getRound() == round);
        assert (ByteUtil.toHexString(commitProof2.getBlockIdentifier().getHash()).equals(blockHashStr));
        assert (commitProof2.getBlockIdentifier().getNumber() == blockNum);
        assert (ByteUtil.toHexString(commitProof2.getCoinbase()).equals(minerCoinbaseStr));
        assert (ByteUtil.toHexString(commitProof2.getVrfPk()).equals(ByteUtil.toHexString(vrfPk)));

        List<CommitProof> commitProof3 = VrfUtil.parseCommitProofs(VrfUtil.readCommitProofsFromFile(filePath + "abc"));
        assert (commitProof3 == null);
    }

    private void genCommitProofsCache() throws IOException {
        CommitProof reductionCommitProof = VrfUtil.genCommitlProof(blockNum, round, seedStr, minerCoinbaseStr,
                blockHashStr, vrfSk, vrfPk, VrfProof.ROLE_CODES_REDUCTION_COMMIT);

        HashMapDB<CommitProof> reductionCommitProofs = new HashMapDB<CommitProof>();
        reductionCommitProofs.put(minerCoinbase, reductionCommitProof);

        VrfUtil.writeReductionCommitProofsToFile(reductionCommitProofs, vrfConfig);

        CommitProof finalCommitProof = VrfUtil.genCommitlProof(blockNum, round, seedStr, minerCoinbaseStr, blockHashStr,
                vrfSk, vrfPk, VrfProof.ROLE_CODES_REDUCTION_COMMIT);

        HashMapDB<CommitProof> finalCommitProofs = new HashMapDB<CommitProof>();
        finalCommitProofs.put(minerCoinbase, finalCommitProof);

        VrfUtil.writeFinalCommitProofsToFile(finalCommitProofs, vrfConfig);
    }

    @Test
    public void testBlockRLPMessage() throws IOException {
        Header header = Header.builder().version(version).hashPrev(prevHash).transactionsRoot(PoAConstants.ZERO_BYTES)
                .height(blockNum).createdAt(System.currentTimeMillis() / 1000).build();
        // Create a new block and set fields.
        Block block = new Block(header);

        byte[] encoded = VrfUtil.genPayload(blockNum, round, seedStr, minerCoinbaseStr, priorityStr, blockHashStr,
                vrfSk, vrfPk, vrfConfig);
        HexBytes payload = HexBytes.fromBytes(encoded);
        block.setPayload(payload);
        block.setHash(HASH_POLICY.getHash(block));

        // Build block message bytes.
        byte[] blockMsgEncoded = VrfUtil.buildMessageBytes(VrfMessageCode.VRF_BLOCK, block);

        // Parse block message bytes.
        VrfMessageCodeAndBytes codeAndBytes = VrfUtil.parseMessageBytes(blockMsgEncoded);
        VrfMessageCode code = codeAndBytes.getCode();
        assert (code == VrfMessageCode.VRF_BLOCK);
        byte[] vrfBytes = codeAndBytes.getRlpBytes();

        // Decode block from message bytes.
        Block blockDecoded = RLPCodec.decode(vrfBytes, Block.class);

        // Assertions.
        assert (block.getHashPrev().equals(blockDecoded.getHashPrev()));
        assert (block.getPayload().equals(blockDecoded.getPayload()));
        assert (blockDecoded.getHash() != null);
        assert (block.getHash().equals(blockDecoded.getHash()));
    }
}
