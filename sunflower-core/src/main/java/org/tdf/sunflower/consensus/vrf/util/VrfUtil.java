package org.tdf.sunflower.consensus.vrf.util;

import org.tdf.common.util.HexBytes;
import org.tdf.crypto.PrivateKey;
import org.tdf.crypto.ed25519.Ed25519;
import org.tdf.crypto.ed25519.Ed25519PrivateKey;
import org.tdf.rlp.RLPCodec;
import org.tdf.rlp.RLPElement;
import org.tdf.rlp.RLPItem;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.consensus.vrf.VrfConfig;
import org.tdf.sunflower.consensus.vrf.core.BlockIdentifier;
import org.tdf.sunflower.consensus.vrf.core.ProposalProof;
import org.tdf.sunflower.consensus.vrf.core.VrfBlockWrapper;
import org.tdf.sunflower.consensus.vrf.core.VrfProof;
import org.tdf.sunflower.consensus.vrf.keystore.FileSystemKeystore;
import org.tdf.sunflower.consensus.vrf.struct.VrfBlockFields;
import org.tdf.sunflower.consensus.vrf.struct.VrfPrivateKey;
import org.tdf.sunflower.consensus.vrf.struct.VrfResult;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.util.ByteUtil;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VrfUtil {
    public static String VRF_PK = "c42e9a44063bfd0956da144d6500ca05351507f55f2490b3966c78a4d7e096ca";
    public static final String VRF_SK = "065994b6ccef45a1dcabbbd77cc11638308142b8c50e08845c3f1e0eeefa8dee";

    // for testing purposes when the timer might be changed
    // to manage current time according to test scenarios
    public static Timer TIMER = new Timer();

    public static class Timer {
        public long curTime() {
            return System.currentTimeMillis();
        }
    }

    public static long curTime() {
        return TIMER.curTime();
    }

    public static byte[] getMiner(HexBytes payload) {
        byte[] encoded = payload.getBytes();
        VrfBlockFields vrfBlockFields = RLPCodec.decode(encoded, VrfBlockFields.class);
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
        VrfBlockFields vrfBlockFields = RLPCodec.decode(encoded, VrfBlockFields.class);
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
        VrfBlockFields vrfBlockFields = RLPCodec.decode(encoded, VrfBlockFields.class);
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

    public static byte[] getPriority(HexBytes payload) {
        byte[] encoded = payload.getBytes();
        VrfBlockFields vrfBlockFields = RLPCodec.decode(encoded, VrfBlockFields.class);
        return vrfBlockFields.getPriority();
    }

    public static byte[] getPriority(Header header) {
        return getPriority(header.getPayload());
    }

    public static byte[] getPriority(Block block) {
        return getPriority(block.getPayload());
    }

    public static void setNonce(Header header, byte[] nonce) {
        HexBytes payload = header.getPayload();
        VrfBlockFields vrfBlockFields = getVrfBlockFields(payload);
        vrfBlockFields.setNonce(nonce);

        byte[] encoded = RLPCodec.encode(vrfBlockFields);
        header.setPayload(HexBytes.fromBytes(encoded));
    }

    public static void setNonce(Block block, byte[] nonce) {
        setNonce(block.getHeader(), nonce);
    }

    public static void setNonce(Block block, HexBytes nonce) {
        setNonce(block, nonce.getBytes());
    }

    public static void setNonce(Block block, String nonce) {
        setNonce(block, ByteUtil.hexStringToBytes(nonce));
    }

    public static void setMiner(Header header, byte[] miner) {
        HexBytes payload = header.getPayload();
        VrfBlockFields vrfBlockFields = getVrfBlockFields(payload);
        vrfBlockFields.setMiner(miner);

        byte[] encoded = RLPCodec.encode(vrfBlockFields);
        header.setPayload(HexBytes.fromBytes(encoded));
    }

    public static void setMiner(Block block, byte[] miner) {
        setMiner(block.getHeader(), miner);
    }

    public static void setMiner(Block block, String miner) {
        setMiner(block, ByteUtil.hexStringToBytes(miner));
    }

    public static void setPriority(Header header, byte[] priority) {
        HexBytes payload = header.getPayload();
        VrfBlockFields vrfBlockFields = getVrfBlockFields(payload);
        vrfBlockFields.setPriority(priority);

        byte[] encoded = RLPCodec.encode(vrfBlockFields);
        header.setPayload(HexBytes.fromBytes(encoded));
    }

    public static void setPriority(Block block, byte[] priority) {
        setPriority(block.getHeader(), priority);
    }

    public static void setPriority(Block block, HexBytes priority) {
        setPriority(block, priority.getBytes());
    }

    public static void setPriority(Block block, String priority) {
        setPriority(block, ByteUtil.hexStringToBytes(priority));
    }

    public static void setProposalProof(Header header, ProposalProof proposalProof) {
        HexBytes payload = header.getPayload();
        VrfBlockFields vrfBlockFields = getVrfBlockFields(payload);
        vrfBlockFields.setProposalProof(proposalProof.getEncoded());

        byte[] encoded = RLPCodec.encode(vrfBlockFields);
        header.setPayload(HexBytes.fromBytes(encoded));
    }

    public static void setProposalProof(Block block, ProposalProof proposalProof) {
        setProposalProof(block.getHeader(), proposalProof);
    }

    public static VrfBlockFields getVrfBlockFields(HexBytes payload) {
        if (payload == null || payload.size() == 0) {
            return VrfBlockFields.builder().nonce(null).priority(null).miner(null).proposalProof(null).build();
        }
        byte[] encoded = payload.getBytes();
        VrfBlockFields vrfBlockFields = RLPCodec.decode(encoded, VrfBlockFields.class);
        return vrfBlockFields;
    }

    // -----> Need to be implemented
//    public static VrfPrivateKey getVrfPrivateKey() {
//        Ed25519PrivateKey skEd25519 = new Ed25519PrivateKey(ByteUtil.hexStringToBytes(VRF_SK));
//        VrfPrivateKey sk = new VrfPrivateKey(skEd25519);
//        return sk;
//    }

    public static VrfPrivateKey getVrfPrivateKey(String vrfDataDir) {
        final String password = "SilkChain@2019@ChangZhou@China#Linux";
        final String vrfPkDir = vrfDataDir + "/keystore";

        PrivateKey key = null;
        FileSystemKeystore fileSystemKeystore = new FileSystemKeystore(vrfPkDir);

        String[] pubkeys = fileSystemKeystore.listStoredKeys();
        if (pubkeys.length == 0) {
            log.info("There is no Vrf SK exist, create a new one and save it in keystore, {}",
                    fileSystemKeystore.getKeyStoreLocation().toAbsolutePath());

            key = new VrfPrivateKey(Ed25519.getAlgorithm()).getSigner();
            fileSystemKeystore.storeKey(key, password);
        } else {
            // We use the first private key as default one
            key = fileSystemKeystore.loadStoredKey(pubkeys[0], password);
            if (key == null) {
                log.error("Fail to load Vrf SK from keystore, create a new one and save it in keystore, {}",
                        fileSystemKeystore.getKeyStoreLocation().toAbsolutePath());

                key = new VrfPrivateKey(Ed25519.getAlgorithm()).getSigner();
                fileSystemKeystore.storeKey(key, password);
            } else {
                log.info("Vrf Sk is loaded from keystore, {}, pubkey {}",
                        fileSystemKeystore.getKeyStoreLocation().toAbsolutePath(), pubkeys[0]);
            }
        }

        return new VrfPrivateKey(key);
    }

    public static VrfPrivateKey getVrfPrivateKey(VrfConfig vrfConfig) {
        return getVrfPrivateKey(vrfConfig.getVrfDataDir());
    }

    public static byte[] genPayload(long blockNum, int round, String nonceStr, String minerCoinbaseStr,
            String priorityStr, String blockHashStr, VrfPrivateKey vrfSk, byte[] vrfPk) {
        byte[] priority = ByteUtil.hexStringToBytes(priorityStr);
        byte[] nonce = ByteUtil.hexStringToBytes(nonceStr);
        byte[] blockHash = ByteUtil.hexStringToBytes(blockHashStr);
        byte[] minerCoinbase = ByteUtil.hexStringToBytes(minerCoinbaseStr);
        return genPayload(blockNum, round, nonce, minerCoinbase, priority, blockHash, vrfSk, vrfPk);
    }

    public static byte[] genPayload(long blockNum, int round, byte[] nonce, byte[] minerCoinbase, byte[] priority,
            byte[] blockHash, VrfPrivateKey vrfSk, byte[] vrfPk) {
        VrfBlockFields vbf1 = genVrfBlockFields(blockNum, round, nonce, minerCoinbase, priority, blockHash, vrfSk,
                vrfPk);

        byte[] encoded = RLPCodec.encode(vbf1);
        return encoded;
    }

    public static byte[] genPayload(long blockNum, int round, HexBytes nonce, HexBytes minerCoinbase,
            HexBytes priority, HexBytes blockHash, VrfPrivateKey vrfSk, byte[] vrfPk) {
        VrfBlockFields vbf1 = genVrfBlockFields(blockNum, round, nonce.getBytes(), minerCoinbase.getBytes(),
                priority.getBytes(), blockHash.getBytes(), vrfSk, vrfPk);

        byte[] encoded = RLPCodec.encode(vbf1);
        return encoded;
    }

    public static VrfBlockFields genVrfBlockFields(long blockNum, int round, String nonceStr, String minerCoinbaseStr,
            String priorityStr, String blockHashStr, VrfPrivateKey vrfSk, byte[] vrfPk) {
        byte[] priority = ByteUtil.hexStringToBytes(priorityStr);
        byte[] nonce = ByteUtil.hexStringToBytes(nonceStr);
        byte[] blockHash = ByteUtil.hexStringToBytes(blockHashStr);
        byte[] minerCoinbase = ByteUtil.hexStringToBytes(minerCoinbaseStr);

        return genVrfBlockFields(blockNum, round, nonce, minerCoinbase, priority, blockHash, vrfSk, vrfPk);
    }

    public static VrfBlockFields genVrfBlockFields(long blockNum, int round, byte[] nonce, byte[] minerCoinbase,
            byte[] priority, byte[] blockHash, VrfPrivateKey vrfSk, byte[] vrfPk) {

        ProposalProof proposalProof = genProposalProof(blockNum, round, nonce, minerCoinbase, blockHash, vrfSk, vrfPk);

        VrfBlockFields vbf1 = VrfBlockFields.builder().nonce(nonce).priority(priority)
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

    public static VrfPrivateKey getVrfPrivateKeyDummy() {
        Ed25519PrivateKey skEd25519 = new Ed25519PrivateKey(ByteUtil.hexStringToBytes(VRF_SK));
        VrfPrivateKey sk = new VrfPrivateKey(skEd25519);
        return sk;
    }

    public static byte[] buildMessageBytes(VrfMessageCode code, Object object) {
        return buildMessageBytes(code.ordinal(), object);
    }

    private static byte[] buildMessageBytes(int code, Object object) {
        RLPList list = RLPElement.readRLPTree(object).asRLPList();
        list.add(0, RLPItem.fromInt(code));
        return list.getEncoded();
    }

    @Getter
    public static class VrfMessageCodeAndBytes {
        VrfMessageCode code;
        byte[] rlpBytes;

        public VrfMessageCodeAndBytes(VrfMessageCode code, byte[] rlpBytes) {
            this.code = code;
            this.rlpBytes = rlpBytes;
        }
    }

    public static VrfMessageCodeAndBytes parseMessageBytes(byte[] msg) {
        RLPList list = RLPElement.fromEncoded(msg).asRLPList();
        int codeInt = list.get(0).asInt();
        list = list.subList(1, list.size());
        VrfMessageCode[] vrfMsgCodes = VrfMessageCode.values();
        VrfMessageCode code = vrfMsgCodes[codeInt];
        return new VrfMessageCodeAndBytes(code, list.getEncoded());
    }
}
