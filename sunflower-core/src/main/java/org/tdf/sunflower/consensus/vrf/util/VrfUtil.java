package org.tdf.sunflower.consensus.vrf.util;

import org.tdf.common.Block;
import org.tdf.common.Header;
import org.tdf.common.HexBytes;
import org.tdf.crypto.ed25519.Ed25519PrivateKey;
import org.tdf.serialize.RLPDeserializer;
import org.tdf.serialize.RLPSerializer;
import org.tdf.sunflower.consensus.poa.PoAUtils;
import org.tdf.sunflower.consensus.vrf.core.BlockIdentifier;
import org.tdf.sunflower.consensus.vrf.core.ProposalProof;
import org.tdf.sunflower.consensus.vrf.core.VrfBlockWrapper;
import org.tdf.sunflower.consensus.vrf.core.VrfProof;
import org.tdf.sunflower.consensus.vrf.struct.VrfBlockFields;
import org.tdf.sunflower.consensus.vrf.struct.VrfPrivateKey;
import org.tdf.sunflower.consensus.vrf.struct.VrfResult;
import org.tdf.sunflower.util.ByteUtil;

public class VrfUtil {
    public static final String VRF_PK = "c42e9a44063bfd0956da144d6500ca05351507f55f2490b3966c78a4d7e096ca";
    public static final String VRF_SK = "065994b6ccef45a1dcabbbd77cc11638308142b8c50e08845c3f1e0eeefa8dee";

    public static byte[] getMiner(HexBytes payload) {
        byte[] encoded = payload.getBytes();
        VrfBlockFields vrfBlockFields = RLPDeserializer.deserialize(encoded, VrfBlockFields.class);
        return vrfBlockFields.getMiner();
    }

    public static byte[] getMiner(Header header) {
        return getMiner(header.getPayload());
    }

    public static byte[] getMiner(Block block) {
        return getMiner(block.getPayload());
    }

    public static ProposalProof getProposalProof(HexBytes payload) {
        byte[] encoded = payload.getBytes();
        VrfBlockFields vrfBlockFields = RLPDeserializer.deserialize(encoded, VrfBlockFields.class);
        return new ProposalProof(vrfBlockFields.getProposalProof());
    }

    public static ProposalProof getProposalProof(Header header) {
        return getProposalProof(header.getPayload());
    }

    public static ProposalProof getProposalProof(Block block) {
        return getProposalProof(block.getPayload());
    }

    public static byte[] getNonce(HexBytes payload) {
        byte[] encoded = payload.getBytes();
        VrfBlockFields vrfBlockFields = RLPDeserializer.deserialize(encoded, VrfBlockFields.class);
        return vrfBlockFields.getNonce();
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

    public static byte[] getDifficulty(HexBytes payload) {
        byte[] encoded = payload.getBytes();
        VrfBlockFields vrfBlockFields = RLPDeserializer.deserialize(encoded, VrfBlockFields.class);
        return vrfBlockFields.getDifficulty();
    }

    public static byte[] getDifficulty(Header header) {
        return getDifficulty(header.getPayload());
    }

    public static byte[] getDifficulty(Block block) {
        return getDifficulty(block.getPayload());
    }

    public static void setNonce(Block block, byte[] nonce) {
        HexBytes payload = block.getPayload();
        VrfBlockFields vrfBlockFields = getVrfBlockFields(payload);
        vrfBlockFields.setNonce(nonce);

        byte[] encoded = RLPSerializer.SERIALIZER.serialize(vrfBlockFields);
        block.setPayload(new HexBytes(encoded));
    }

    public static void setNonce(Block block, HexBytes nonce) {
        setNonce(block, nonce.getBytes());
    }

    public static void setNonce(Block block, String nonce) {
        setNonce(block, ByteUtil.hexStringToBytes(nonce));
    }

    public static void setMiner(Block block, byte[] miner) {
        HexBytes payload = block.getPayload();
        VrfBlockFields vrfBlockFields = getVrfBlockFields(payload);
        vrfBlockFields.setMiner(miner);

        byte[] encoded = RLPSerializer.SERIALIZER.serialize(vrfBlockFields);
        block.setPayload(new HexBytes(encoded));
    }

    public static void setMiner(Block block, String miner) {
        setMiner(block, ByteUtil.hexStringToBytes(miner));
    }

    public static void setDifficulty(Block block, byte[] difficulty) {
        HexBytes payload = block.getPayload();
        VrfBlockFields vrfBlockFields = getVrfBlockFields(payload);
        vrfBlockFields.setDifficulty(difficulty);

        byte[] encoded = RLPSerializer.SERIALIZER.serialize(vrfBlockFields);
        block.setPayload(new HexBytes(encoded));
    }

    public static void setDifficulty(Block block, HexBytes difficulty) {
        setDifficulty(block, difficulty.getBytes());
    }

    public static void setDifficulty(Block block, String difficulty) {
        setDifficulty(block, ByteUtil.hexStringToBytes(difficulty));
    }

    public static void setProposalProof(Block block, ProposalProof proposalProof) {
        HexBytes payload = block.getPayload();
        VrfBlockFields vrfBlockFields = getVrfBlockFields(payload);
        vrfBlockFields.setProposalProof(proposalProof.getEncoded());

        byte[] encoded = RLPSerializer.SERIALIZER.serialize(vrfBlockFields);
        block.setPayload(new HexBytes(encoded));
    }

    public static VrfBlockFields getVrfBlockFields(HexBytes payload) {
        if (payload == null || payload.size() == 0) {
            return VrfBlockFields.builder().nonce(null).difficulty(null).miner(null).proposalProof(null).build();
        }
        byte[] encoded = payload.getBytes();
        VrfBlockFields vrfBlockFields = RLPDeserializer.deserialize(encoded, VrfBlockFields.class);
        return vrfBlockFields;
    }

    // -----> Need to be implemented
    public static VrfPrivateKey getVrfPrivateKey() {
        Ed25519PrivateKey skEd25519 = new Ed25519PrivateKey(ByteUtil.hexStringToBytes(VRF_SK));
        VrfPrivateKey sk = new VrfPrivateKey(skEd25519);
        return sk;
    }

    public static byte[] genPayload(long blockNum, int round, String nonceStr, String minerCoinbaseStr,
            String difficultyStr, String blockHashStr, VrfPrivateKey vrfSk, byte[] vrfPk) {
        byte[] difficulty = ByteUtil.hexStringToBytes(difficultyStr);
        byte[] nonce = ByteUtil.hexStringToBytes(nonceStr);
        byte[] blockHash = ByteUtil.hexStringToBytes(blockHashStr);
        byte[] minerCoinbase = ByteUtil.hexStringToBytes(minerCoinbaseStr);
        return genPayload(blockNum, round, nonce, minerCoinbase, difficulty, blockHash, vrfSk, vrfPk);
    }

    public static byte[] genPayload(long blockNum, int round, byte[] nonce, byte[] minerCoinbase, byte[] difficulty,
            byte[] blockHash, VrfPrivateKey vrfSk, byte[] vrfPk) {
        VrfBlockFields vbf1 = genVrfBlockFields(blockNum, round, nonce, minerCoinbase, difficulty, blockHash, vrfSk,
                vrfPk);

        byte[] encoded = RLPSerializer.SERIALIZER.serialize(vbf1);
        return encoded;
    }

    public static byte[] genPayload(long blockNum, int round, HexBytes nonce, HexBytes minerCoinbase,
            HexBytes difficulty, HexBytes blockHash, VrfPrivateKey vrfSk, byte[] vrfPk) {
        VrfBlockFields vbf1 = genVrfBlockFields(blockNum, round, nonce.getBytes(), minerCoinbase.getBytes(),
                difficulty.getBytes(), blockHash.getBytes(), vrfSk, vrfPk);

        byte[] encoded = RLPSerializer.SERIALIZER.serialize(vbf1);
        return encoded;
    }

    public static VrfBlockFields genVrfBlockFields(long blockNum, int round, String nonceStr, String minerCoinbaseStr,
            String difficultyStr, String blockHashStr, VrfPrivateKey vrfSk, byte[] vrfPk) {
        byte[] difficulty = ByteUtil.hexStringToBytes(difficultyStr);
        byte[] nonce = ByteUtil.hexStringToBytes(nonceStr);
        byte[] blockHash = ByteUtil.hexStringToBytes(blockHashStr);
        byte[] minerCoinbase = ByteUtil.hexStringToBytes(minerCoinbaseStr);

        return genVrfBlockFields(blockNum, round, nonce, minerCoinbase, difficulty, blockHash, vrfSk, vrfPk);
    }

    public static VrfBlockFields genVrfBlockFields(long blockNum, int round, byte[] nonce, byte[] minerCoinbase,
            byte[] difficulty, byte[] blockHash, VrfPrivateKey vrfSk, byte[] vrfPk) {

        ProposalProof proposalProof = genProposalProof(blockNum, round, nonce, minerCoinbase, blockHash, vrfSk, vrfPk);

        VrfBlockFields vbf1 = VrfBlockFields.builder().nonce(nonce).difficulty(difficulty)
                .proposalProof(proposalProof.getEncoded()).miner(minerCoinbase).build();
        return vbf1;
    }

    public static ProposalProof genProposalProof(long blockNum, int round, String nonceStr, String minerCoinbaseStr,
            String blockHashStr, VrfPrivateKey vrfSk, byte[] vrfPk) {
        byte[] nonce = ByteUtil.hexStringToBytes(nonceStr);
        byte[] blockHash = ByteUtil.hexStringToBytes(blockHashStr);
        byte[] minerCoinbase = ByteUtil.hexStringToBytes(minerCoinbaseStr);
        return genProposalProof(blockNum, round, nonce, minerCoinbase, blockHash, vrfSk, vrfPk);
    }

    public static ProposalProof genProposalProof(long blockNum, int round, byte[] nonce, byte[] minerCoinbase,
            byte[] blockHash, VrfPrivateKey vrfSk, byte[] vrfPk) {
        BlockIdentifier blockIdentifier = new BlockIdentifier(blockHash, blockNum);

        VrfResult vrfResult = VrfProof.Util.prove(VrfProof.ROLE_CODES_PROPOSER, round, vrfSk, nonce);
        VrfProof vrfProof = VrfProof.Util.vrfProof(VrfProof.ROLE_CODES_PROPOSER, round, vrfPk, nonce, vrfResult);

        ProposalProof proposalProof = new ProposalProof(vrfProof, minerCoinbase, blockIdentifier, vrfSk.getSigner());
        return proposalProof;
    }
}
