package org.tdf.sunflower.consensus.vrf.util;

import org.tdf.common.Block;
import org.tdf.common.Header;
import org.tdf.common.HexBytes;
import org.tdf.crypto.ed25519.Ed25519PrivateKey;
import org.tdf.serialize.RLPDeserializer;
import org.tdf.serialize.RLPSerializer;
import org.tdf.sunflower.consensus.vrf.core.ProposalProof;
import org.tdf.sunflower.consensus.vrf.core.VrfBlockWrapper;
import org.tdf.sunflower.consensus.vrf.struct.VrfBlockFields;
import org.tdf.sunflower.consensus.vrf.struct.VrfPrivateKey;
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
        if(payload == null) {
            return VrfBlockFields.builder().nonce(null).difficulty(null).miner(null).proposalProof(null).build();
        }
        byte[] encoded = payload.getBytes();
        VrfBlockFields vrfBlockFields = RLPDeserializer.deserialize(encoded, VrfBlockFields.class);
        return vrfBlockFields;
    }

    //-----> Need to be implemented
    public static VrfPrivateKey getVrfPrivateKey() {
        Ed25519PrivateKey skEd25519 = new Ed25519PrivateKey(ByteUtil.hexStringToBytes(VRF_SK));
        VrfPrivateKey sk = new VrfPrivateKey(skEd25519);
        return sk;
    }

}
